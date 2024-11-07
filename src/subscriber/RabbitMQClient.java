package subscriber;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQClient {
    private static final String MESSAGE_QUEUE = "logs";
    private final String HOST_IP;
    private String queueName;
    private Connection connection;
    private Channel channel;

    // Constructor
    public RabbitMQClient(String host) throws Exception {
        this.HOST_IP = host;
        setupConnection();
    }

    // Getters
    public static String getMessageQueue() {
        return MESSAGE_QUEUE;
    }

    public Connection getConnection() {
        return connection;
    }

    public Channel getChannel() {
        return channel;
    }

    // Setters
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    /**
     * Sets up the connection with the RabbitMQ server
     * @throws Exception
     */
    public void setupConnection() throws Exception {
        // Close existing connection and channel if they are open
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        if (channel != null && channel.isOpen()) {
            channel.close();
        }

        // Create a new connection and channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.HOST_IP);
        this.connection = factory.newConnection();
        this.channel = connection.createChannel();
    }

    /**
     * Sets up the exchange on the RabbitMQ server
     * @throws Exception
     */
    public void setupExchange() throws Exception {
        channel.exchangeDeclare(MESSAGE_QUEUE, "fanout");
        System.out.println(" [*] Connected to exchange " + MESSAGE_QUEUE);
    }

    /**
     * Closes the connection and the channel with the RabbitMQ server
     * @throws Exception
     */
    public void close() throws Exception {
        if (channel != null && channel.isOpen()) {
            channel.queueDelete(queueName);
            channel.close();
        }
        if (connection.isOpen()) connection.close();
    }
}
