package suscriber;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQClient {
    private static final String MESSAGE_QUEUE = "logs";
    private final String HOST_IP;
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


    /**
     * Configura la conexión con el servidor RabbitMQ
     * @throws Exception
     */
    public void setupConnection() throws Exception {
        // Cerrar conexión y canal existentes si están abiertos
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        if (channel != null && channel.isOpen()) {
            channel.close();
        }

        // Crear una nueva conexión y canal
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.HOST_IP);
        this.connection = factory.newConnection();
        this.channel = connection.createChannel();
    }

    /**
     * Configura el exchange en el servidor RabbitMQ
     * @throws Exception
     */
    public void setupExchange() throws Exception {
        channel.exchangeDeclare(MESSAGE_QUEUE, "fanout");
        System.out.println(" [*] Conectado al exchange " + MESSAGE_QUEUE);
    }

    /**
     * Cierra la conexión y el canal con el servidor RabbitMQ
     * @throws Exception
     */
    public void close() throws Exception {
        if (channel.isOpen()) channel.close();
        if (connection.isOpen()) connection.close();
    }
}
