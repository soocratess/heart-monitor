package subscriber;

import com.rabbitmq.client.AMQP;

import java.nio.charset.StandardCharsets;

public class ClientRegistrer {
    private static final String REGISTRATION_QUEUE = "registration_queue";
    private RabbitMQClient rabbitMQClient;

    public ClientRegistrer(RabbitMQClient rabbitMQClient) {
        this.rabbitMQClient = rabbitMQClient;
    }

    public RabbitMQClient getRabbitMQClient() {
        return rabbitMQClient;
    }

    /**
     * Registers a client on the server with a message limit
     * @param messageLimit Limit of messages to receive
     * @return Name of the registered queue
     * @throws Exception
     */
    public String registerClient(int messageLimit) throws Exception {
        // Dynamic queue name based on the current time in milliseconds
        String queueName = "rabbit.client-" + System.currentTimeMillis();
        rabbitMQClient.getChannel().queueDeclare(queueName, false, false, false, null);
        rabbitMQClient.getChannel().queueBind(queueName, RabbitMQClient.getMessageQueue(), "");
        rabbitMQClient.setQueueName(queueName);

        // Send the message limit to the server for registration
        String message = Integer.toString(messageLimit);
        rabbitMQClient.getChannel().basicPublish("", REGISTRATION_QUEUE, new AMQP.BasicProperties.Builder()
                .replyTo(queueName)
                .build(), message.getBytes(StandardCharsets.UTF_8));

        System.out.println(" [*] Message limit (" + messageLimit + ") sent to the server for queue: " + queueName);
        return queueName;
    }

    /**
     * Re-registers a client on the server with a new message limit
     * @param messageLimit New message limit to receive
     * @return Name of the new registered queue
     * @throws Exception
     */
    public String reRegisterClient(int messageLimit) throws Exception {
        // Close resources if they are open
        if (rabbitMQClient.getConnection().isOpen() || rabbitMQClient.getChannel().isOpen()) {
            rabbitMQClient.close();
        }

        // Reconfigure the connection and register the queue again
        rabbitMQClient.setupConnection();
        String queueName = registerClient(messageLimit);
        System.out.println(" [*] Re-registered with new queue: " + queueName);

        // Return the name of the new queue
        return queueName;
    }
}
