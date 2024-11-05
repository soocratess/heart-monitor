package suscriber;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQClient {
    private static final String EXCHANGE_NAME = "logs";
    private final String HOST_IP;
    private Connection connection;
    private Channel channel;

    public static String getExchangeName() {
        return EXCHANGE_NAME;
    }

    public RabbitMQClient(String host) throws Exception {
        this.HOST_IP = host;
        setupConnection();
    }

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


    public void setupExchange() throws Exception {
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        System.out.println(" [*] Conectado al exchange " + EXCHANGE_NAME);
    }

    public Connection getConnection() {
        return connection;
    }

    public Channel getChannel() {
        return channel;
    }

    public void close() throws Exception {
        channel.close();
        connection.close();
    }
}
