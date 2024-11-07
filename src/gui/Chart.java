package gui;

import javax.swing.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.category.DefaultCategoryDataset;
import subscriber.ClientRegistrer;
import subscriber.MessageSubscriber;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Deque;
import java.util.LinkedList;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;


/**
 * This class contains the code for the ECG GUI with a countdown timer and a "Renew" button.
 */
public class Chart extends JFrame {
    private static final long serialVersionUID = 1L;
    private DefaultCategoryDataset dataset;
    private Deque<String> timeStamps;
    private static final int MAX_POINTS = 60;

    // Variables for the countdown timer and GUI components
    private int timeLeft;
    private JLabel counterLabel;
    private JButton renewButton;
    private JTextField inputField;

    // Variable for the client registrer
    private ClientRegistrer clientRegistrer;

    public int getTimeLeft() {
        return timeLeft;
    }

    public Chart(int messageLimit) {
        super("Heart Monitor");

        // Window configuration
        this.timeLeft = messageLimit;
        this.dataset = new DefaultCategoryDataset();
        this.timeStamps = new LinkedList<>();

        // Create the main panel with the chart and the counter panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createChartPanel(), BorderLayout.CENTER);
        mainPanel.add(createCounterPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setSize(800, 600);

        // Close resources when the window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeResources();  // Close resources when the window is closed
                dispose();
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // Setters and getters
    public void setClientRegistrer(ClientRegistrer clientRegistrer) {
        this.clientRegistrer = clientRegistrer;
    }

    public ClientRegistrer getClientRegistrer() {
        return clientRegistrer;
    }

    /**
     * Method to close resources when the window is closed.
     */
    private void closeResources() {
        if (clientRegistrer != null) {
            try {
                clientRegistrer.getRabbitMQClient().close(); // Close the RabbitMQ client
                System.out.println("Resources closed successfully.");
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates the chart panel with the ECG graph.
     * @return ChartPanel with the ECG graph.
     */
    private ChartPanel createChartPanel() {
        JFreeChart chart = ChartFactory.createLineChart(
                "Heart Monitor", "time (s)", "bpm", dataset
        );

        // Personalize the chart
        CategoryPlot plot = chart.getCategoryPlot();
        CategoryAxis xAxis = plot.getDomainAxis();

        // Configure the X axis
        xAxis.setTickLabelsVisible(true);
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90); // Rotate the labels 90 degrees
        xAxis.setTickLabelPaint(Color.GRAY); // Change the color of the labels
        xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        xAxis.setMaximumCategoryLabelWidthRatio(2.0f);

        // Change the background color of the plot
        plot.setBackgroundPaint(new Color(240, 240, 240)); // Light gray

        // Configure the Y axis
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(1.0, 2.3); // Set the range of the Y axis

        return new ChartPanel(chart);
    }


    /**
     * Creates the control panel with the countdown timer, the "Renew" button, and the input field.
     * @return JPanel that contains the counter and the renew button.
     */
    private JPanel createCounterPanel() {
        counterLabel = new JLabel("Counter: " + timeLeft);
        renewButton = new JButton("Renew");
        inputField = new JTextField(5);

        renewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String inputText = inputField.getText();
                if (!inputText.isEmpty()) {
                    int newMessageLimit = Integer.parseInt(inputText);
                    setTimeLeft(newMessageLimit);  // Set the new time limit

                    try {
                        // Re-register the client and restart the subscription
                        String newQueueName = clientRegistrer.reRegisterClient(newMessageLimit);

                        // Restart the subscription with the new queue
                        MessageSubscriber subscriber = new MessageSubscriber(clientRegistrer.getRabbitMQClient(), Chart.this);
                        subscriber.startSubscription(newQueueName, newMessageLimit);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    inputField.setText("");
                }
            }
        });

        JPanel counterPanel = new JPanel();
        counterPanel.add(counterLabel);
        counterPanel.add(renewButton);
        counterPanel.add(inputField);

        return counterPanel;
    }

    // Variable to keep track of the global time count in seconds
    private int timeCounter = 0;

    /**
     * Adds a data point to the graph and reduces the counter.
     *
     * @param bpm Heartbeats per minute value for the Y-axis
     */
    public void addDataPoint(int lineNumber, double bpm) {
        // If there is a gap in the data, fill the space with a value of -1
        if (!timeStamps.isEmpty() && Integer.parseInt(timeStamps.peekLast()) != lineNumber - 1) {
            while (Integer.parseInt(timeStamps.peekLast()) != lineNumber - 1) {
                timeStamps.add(Integer.toString(Integer.parseInt(timeStamps.peekLast()) + 1));
                dataset.addValue(-1, "Heart Rate", timeStamps.peekLast());
            }
        }

        // If the number of points in timeStamps has reached MAX_POINTS, remove the oldest data
        if (timeStamps.size() >= MAX_POINTS) {
            while (timeStamps.size() >= MAX_POINTS) {
                String oldestTime = timeStamps.poll(); // Remove the oldest time from the queue
                dataset.removeValue("Heart Rate", oldestTime); // Remove the corresponding value in the dataset
            }
        }

        // Add the new data point with a non-resetting time label
        String timeLabel = String.valueOf(lineNumber); // Use lineNumber instead of the queue size
        timeStamps.add(timeLabel);
        dataset.addValue(bpm, "Heart Rate", timeLabel);

        // Increment the global time counter
        timeCounter++;

        // Update the remaining time counter
        if (timeLeft > 0) {
            timeLeft--;
        }
        counterLabel.setText("Time left (s): " + timeLeft);
    }

    public void setTimeLeft(int time) {
        timeLeft = time;
        counterLabel.setText("Time left (s): " + timeLeft);
    }
}
