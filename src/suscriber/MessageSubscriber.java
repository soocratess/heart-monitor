package suscriber;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import gui.Chart;

import javax.swing.SwingUtilities;
import java.nio.charset.StandardCharsets;

public class MessageSubscriber {
    private static final String GREEN = "\033[32m";
    private static final String RESET = "\u001B[0m";
    private final Connection connection;
    private final Channel channel;
    private final Chart chart;

    public MessageSubscriber(Connection connection, Channel channel, Chart chart) {
        this.connection = connection;
        this.channel = channel;
        this.chart = chart;
    }

    public void startSubscription(String queueName, int messageLimit) throws Exception {
        final int[] messageCount = {0};

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
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
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }

    private void closeResources(String queueName) {
        try {
            System.out.println(" [*] Se ha alcanzado el límite de mensajes. Desconectando...");
            channel.queueDelete(queueName);
            channel.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
