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
import java.util.LinkedList;
import java.util.Queue;

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
    private Queue<String> timeStamps;
    private static final int MAX_POINTS = 60;

    // Variables para el contador y el panel de entrada
    private int timeLeft;
    private JLabel counterLabel;
    private JButton renewButton;
    private JTextField inputField;

    // Variable para el registro del cliente
    private ClientRegistrer clientRegistrer;

    public int getTimeLeft() {
        return timeLeft;
    }

    public Chart(int messageLimit) {
        super("Heart Monitor");

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
                "Heart Monitor", "time (s)", "bpm", dataset
        );

        // Personalizar el eje X para reducir la cantidad de etiquetas mostradas
        CategoryPlot plot = chart.getCategoryPlot();
        CategoryAxis xAxis = plot.getDomainAxis();

        // Configura el intervalo de etiquetas y estilo de fuente
        xAxis.setTickLabelsVisible(true);
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90); // Girar etiquetas, opcionalmente
        xAxis.setTickLabelPaint(Color.GRAY); // Cambiar color de las etiquetas
        xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        xAxis.setMaximumCategoryLabelWidthRatio(2.0f);

        // Cambiar el fondo del gráfico a un color casi blanco
        plot.setBackgroundPaint(new Color(240, 240, 240)); // Color de fondo casi blanco

        // Configurar el eje Y para centrarlo en torno al valor 1.65
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(1.0, 2.3); // Establecer el rango, de modo que 1.65 esté en el centro


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

    // Variable para llevar el conteo global del tiempo en segundos
    private int timeCounter = 0;

    /**
     * Agrega un punto de datos al gráfico y reduce el contador.
     *
     * @param bpm Valor de latidos por minuto para el eje Y
     */
    public void addDataPoint(double bpm) {
        // Si el número de puntos en timeStamps ha alcanzado MAX_POINTS, elimina el dato más antiguo
        if (timeStamps.size() >= MAX_POINTS) {
            String oldestTime = timeStamps.poll(); // Elimina el tiempo más antiguo de la cola
            dataset.removeValue("Heart Rate", oldestTime); // Elimina el valor correspondiente en el dataset
        }

        // Agrega el nuevo punto de datos con una etiqueta de tiempo que no se reinicia
        String timeLabel = timeCounter-60 + "s"; // Usa timeCounter en lugar del tamaño de la cola
        dataset.addValue(bpm, "Heart Rate", timeLabel);
        timeStamps.add(timeLabel);

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
