package gui;

import javax.swing.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.category.DefaultCategoryDataset;
import suscriber.ClientRegistrer;
import suscriber.MessageSubscriber;

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
 * This class contains the code of the ECG GUI with a countdown timer and a "Renovar" button.
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
                closeResources();  // Cerrar recursos al cerrar la ventana
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
                System.out.println("Recursos cerrados correctamente.");
            } catch (Exception e) {
                System.err.println("Error al cerrar los recursos: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Create the chart panel with the ECG graph.
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
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90); // Turn the labels 90 degrees
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
     * Crea el panel de control con el contador, el botón "Renovar" y el campo de entrada.
     *
     * @return JPanel que contiene el contador y el botón de renovación.
     */
    private JPanel createCounterPanel() {
        counterLabel = new JLabel("Counter: " + timeLeft);
        renewButton = new JButton("Renovar");
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

    // Variable para llevar el conteo global del tiempo en segundos
    private int timeCounter = 0;

    /**
     * Agrega un punto de datos al gráfico y reduce el contador.
     *
     * @param bpm Valor de latidos por minuto para el eje Y
     */
    public void addDataPoint(int lineNumber, double bpm) {
        // Si hay un hueco en los datos, rellena el espacio con un valor de -1
        if (!timeStamps.isEmpty() && Integer.parseInt(timeStamps.peekLast()) != lineNumber-1) {
            while(Integer.parseInt(timeStamps.peekLast()) != lineNumber-1) {
                timeStamps.add(Integer.toString(Integer.parseInt(timeStamps.peekLast())+1));
                dataset.addValue(-1, "Heart Rate", timeStamps.peekLast());
            }
        }

        // Si el número de puntos en timeStamps ha alcanzado MAX_POINTS, elimina el dato más antiguo
        if (timeStamps.size() >= MAX_POINTS) {
            while (timeStamps.size() >= MAX_POINTS) {
                String oldestTime = timeStamps.poll(); // Elimina el tiempo más antiguo de la cola
                dataset.removeValue("Heart Rate", oldestTime); // Elimina el valor correspondiente en el dataset
            }
        }

        // Agrega el nuevo punto de datos con una etiqueta de tiempo que no se reinicia
        String timeLabel = String.valueOf(lineNumber); // Usa timeCounter en lugar del tamaño de la cola
        timeStamps.add(timeLabel);
        dataset.addValue(bpm, "Heart Rate", timeLabel);

        // Incrementa el contador de tiempo global
        timeCounter++;

        // Actualiza el contador de tiempo restante
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
