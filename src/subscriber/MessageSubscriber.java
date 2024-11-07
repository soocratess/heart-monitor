package subscriber;

import com.rabbitmq.client.DeliverCallback;
import gui.Chart;

import javax.swing.SwingUtilities;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
     * Starts the message subscription for the specified queue
     * @param queueName Name of the queue to subscribe to
     * @param messageLimit Limit of messages to receive
     * @throws Exception
     */
    public void startSubscription(String queueName, int messageLimit) throws Exception {

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                // Get the message as a text
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] Received '" + GREEN + message + RESET + "'");

                // Retrieve the line number from the message headers
                Map<String, Object> headers = delivery.getProperties().getHeaders();
                if (headers != null && headers.containsKey("line-number")) {
                    int lineNumber = (int) headers.get("line-number");
                    // Process the message and update the chart
                    SwingUtilities.invokeLater(() -> {
                        try {
                            double bpm = 1D / Double.parseDouble(message);
                            chart.addDataPoint(lineNumber, bpm);
                        } catch (NumberFormatException e) {
                            System.err.println("Error converting message to number: " + message);
                        }
                    });
                } else {
                    System.out.println("Header 'line-number' not found in the message.");
                }

            } catch (Exception e) {
                System.out.println(" [!] Connection or channel closed. Attempting to re-register...");
                reStartSubscription(messageLimit);
            }
        };

        // This is where the client actually subscribes to the queue in RabbitMQ
        rabbitMQClient.getChannel().basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }

    /**
     * Re-registers the client and restarts the subscription
     * @param messageLimit Limit of messages to receive
     */
    private void reStartSubscription(int messageLimit) {
        try {
            // Re-register the client and restart the subscription
            String newQueueName = chart.getClientRegistrer().reRegisterClient(messageLimit);
            startSubscription(newQueueName, messageLimit); // Restart the subscription with the new queue
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
