package suscriber;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQClient {
    private static final String EXCHANGE_NAME = "logs";
    private Connection connection;
    private Channel channel;

    public static String getExchangeName() {
        return EXCHANGE_NAME;
    }

    public RabbitMQClient(String host) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
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
