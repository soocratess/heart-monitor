package suscriber;

import com.rabbitmq.client.DeliverCallback;
import gui.Chart;

import javax.swing.SwingUtilities;

import java.nio.charset.StandardCharsets;

public class MessageSubscriber {
    private static final String GREEN = "\033[32m";
    private static final String RESET = "\u001B[0m";
    private RabbitMQClient rabbitMQClient;
    private final Chart chart;

    public MessageSubscriber(RabbitMQClient rabbitMQClient, Chart chart) {
        this.rabbitMQClient = rabbitMQClient;
        this.chart = chart;
    }

    /**
     * Inicia la suscripción a mensajes en la cola especificada
     * @param queueName Nombre de la cola a suscribir
     * @param messageLimit Límite de mensajes a recibir
     * @throws Exception
     */
    public void startSubscription(String queueName, int messageLimit) throws Exception {

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] Received '" + GREEN + message + RESET + "'");

                    SwingUtilities.invokeLater(() -> {
                        try {
                            double bpm = 1D / Double.parseDouble(message);
                            chart.addDataPoint(bpm);
                        } catch (NumberFormatException e) {
                            System.err.println("Error al convertir el mensaje a número: " + message);
                        }
                    });
            } catch (Exception e) {
                System.out.println(" [!] Conexión o canal cerrado. Intentando re-registrar...");
                reStartSubscription(messageLimit);
            }
        };

        // Aquí es donde el cliente realmente se suscribe a la cola en RabbitMQ
        rabbitMQClient.getChannel().basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }

    /**
     * Re-registra el cliente y reinicia la suscripción
     * @param messageLimit Límite de mensajes a recibir
     */
    private void reStartSubscription(int messageLimit) {
        try {
            // Re-registrar el cliente y reiniciar la suscripción
            String newQueueName = chart.getClientRegistrer().reRegisterClient(messageLimit);
            startSubscription(newQueueName, messageLimit); // Reiniciar la suscripción con la nueva cola
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
