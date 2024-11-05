package gui;

import javax.swing.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import suscriber.ClientRegistrer;
import suscriber.MessageSubscriber;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Queue;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * This class contains the code of the ECG GUI with a countdown timer and a "Renovar" button.
 */
public class Chart extends JFrame {
    private static final long serialVersionUID = 1L;
    private DefaultCategoryDataset dataset;
    private Queue<String> timeStamps;
    private static final int MAX_POINTS = 60;

    // Variables para el contador y el panel de entrada
    private int timeLeft;
    private JLabel counterLabel;
    private JButton renewButton;
    private JTextField inputField;

    // Variable para el registro del cliente
    private ClientRegistrer clientRegistrer;


    public Chart(int messageLimit) {
        super("ECG Monitor");

        // Configuración básica de la ventana y componentes
        this.timeLeft = messageLimit;
        this.dataset = new DefaultCategoryDataset();
        this.timeStamps = new LinkedList<>();

        // Crear el panel principal de la ventana
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createChartPanel(), BorderLayout.CENTER);
        mainPanel.add(createCounterPanel(), BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setSize(800, 600);

        // Agregar un WindowListener para cerrar recursos cuando se cierre la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeResources();  // Cerrar recursos al cerrar la ventana
                dispose();
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);    }

    // Funcion que permite la inyección de dependencias
    public void setClientRegistrer(ClientRegistrer clientRegistrer) {
        this.clientRegistrer = clientRegistrer;
    }

    public ClientRegistrer getClientRegistrer() {
        return clientRegistrer;
    }

    /**
     * Metodo para cerrar todos los recursos al cerrar la aplicación.
     */
    private void closeResources() {
        if (clientRegistrer != null) {
            try {
                clientRegistrer.getRabbitMQClient().close(); // Cerrar la conexión RabbitMQ
                System.out.println("Recursos cerrados correctamente.");
            } catch (Exception e) {
                System.err.println("Error al cerrar los recursos: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Crea y configura el panel del gráfico.
     *
     * @return ChartPanel con el gráfico de ECG.
     */
    private ChartPanel createChartPanel() {
        JFreeChart chart = ChartFactory.createLineChart(
                "Electrocardiogram Monitor", "time (s)", "bpm", dataset
        );
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
                    setTimeLeft(newMessageLimit);  // Actualizar el contador en la GUI

                    try {
                        // Llamar a reRegisterClient y obtener el nuevo nombre de la cola
                        String newQueueName = clientRegistrer.reRegisterClient(newMessageLimit);

                        // Reiniciar la suscripción en el cliente usando el nuevo nombre de cola
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

    /**
     * Agrega un punto de datos al gráfico y reduce el contador.
     *
     * @param bpm Valor de latidos por minuto para el eje Y
     */
    public void addDataPoint(double bpm) {
        String timeLabel = timeStamps.size() + "s";
        dataset.addValue(bpm, "Heart Rate", timeLabel);
        timeStamps.add(timeLabel);

        if (timeStamps.size() > MAX_POINTS) {
            String oldestTime = timeStamps.poll();
            dataset.removeValue("Heart Rate", oldestTime);
        }

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
