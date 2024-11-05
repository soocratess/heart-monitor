package suscriber;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import gui.Chart;

import javax.swing.SwingUtilities;
import java.io.IOException;
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

    public void startSubscription(String queueName, int messageLimit) throws Exception {
        final int[] messageCount = {0};

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                if (messageCount[0] < messageLimit) {
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

                    messageCount[0]++;

                    if (messageCount[0] >= messageLimit) {
                        closeResources(queueName);
                    }
                }
            } catch (Exception e) {
                System.out.println(" [!] Conexión o canal cerrado. Intentando re-registrar...");
                reRegisterSubscription(messageLimit);
            }
        };

        rabbitMQClient.getChannel().basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }

    private void reRegisterSubscription(int messageLimit) {
        try {
            // Re-registrar el cliente y reiniciar la suscripción
            String newQueueName = chart.getClientRegistrer().reRegisterClient(messageLimit);
            startSubscription(newQueueName, messageLimit); // Reiniciar la suscripción con la nueva cola
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void closeResources(String queueName) {
        try {
            if (rabbitMQClient.getChannel().isOpen()) rabbitMQClient.getChannel().queueDelete(queueName);
            if (rabbitMQClient.getChannel().isOpen()) rabbitMQClient.getChannel().close();
            if (rabbitMQClient.getConnection().isOpen()) rabbitMQClient.getConnection().close();
            System.out.println(" [*] Se ha alcanzado el límite de mensajes. Desconectado del servidor.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
