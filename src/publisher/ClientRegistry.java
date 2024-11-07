package publisher;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.HashMap;

/**
 * This class is used to register clients and handle their queues.
 */
public class ClientRegistry {
    private static final String REGISTRATION_QUEUE = "registration_queue";
    private final Channel connectionChannel; // Exclusive channel for managing clients
    private final Channel publishChannel; // Channel for publishing messages
    private final HashMap<String, ClientQueueInfo> queueInfoMap;

    public ClientRegistry(Channel clientChannel, Channel publishChannel) {
        this.connectionChannel = clientChannel;
        this.publishChannel = publishChannel;
        this.queueInfoMap = new HashMap<>();
    }

    /**
     * Sets up the listener to register clients.
     * @throws IOException
     */
    public void setupRegistrationListener() throws IOException {
        connectionChannel.queueDeclare(REGISTRATION_QUEUE, true, false, false, null);

        DeliverCallback registerCallback = (consumerTag, delivery) -> {
            String queueName = delivery.getProperties().getReplyTo();
            int messageLimit = Integer.parseInt(new String(delivery.getBody(), "UTF-8"));
            addClientQueue(queueName, messageLimit);
        };

        // Callback that is executed when a client disconnects
        CancelCallback disconnectCallback = consumerTag -> {
            System.out.println("Client disconnected with consumerTag: " + consumerTag);
            // Find and remove the client's queue from the map using the consumerTag
            queueInfoMap.entrySet().removeIf(entry -> entry.getKey().equals(consumerTag));
        };

        connectionChannel.basicConsume(REGISTRATION_QUEUE, true, registerCallback, consumerTag -> {});
    }

    /**
     * Adds a client queue to the queue map.
     * @param queueName Name of the client's queue
     * @param messageLimit Message limit for the queue
     */
    public void addClientQueue(String queueName, int messageLimit) {
        queueInfoMap.put(queueName, new ClientQueueInfo(messageLimit));
        System.out.println("Client connected with queue: " + queueName + " and message limit: " + messageLimit);
    }

    /**
     * Manages client queues by removing those that have reached the message limit.
     * @throws IOException
     */
    public void handleClientQueues() throws IOException {
        var iterator = queueInfoMap.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            String queueName = entry.getKey();
            ClientQueueInfo queueInfo = entry.getValue();

            queueInfo.incrementMessageCount();
            if (queueInfo.hasReachedLimit()) {
                publishChannel.queueDelete(queueName); // Delete only the client's queue
                iterator.remove();
                System.out.println("Queue " + queueName + " deleted.");
            }
        }
    }

    /**
     * Checks if there are any connected clients.
     * @return true if there are connected clients, false otherwise
     */
    public boolean hasClients() {
        return !queueInfoMap.isEmpty();
    }

}
