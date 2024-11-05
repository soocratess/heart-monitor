package suscriber;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;

public class ClientRegistrer {
    private static final String REGISTRATION_QUEUE = "registration_queue";
    private RabbitMQClient rabbitMQClient;

    public ClientRegistrer(RabbitMQClient rabbitMQClient) {
        this.rabbitMQClient = rabbitMQClient;
    }

    public String registerClient(int messageLimit) throws Exception {
        String queueName = rabbitMQClient.getChannel().queueDeclare("", false, false, true, null).getQueue();
        rabbitMQClient.getChannel().queueBind(queueName, RabbitMQClient.getExchangeName(), "");

        // Enviar el límite de mensajes al servidor para registrarlo
        String message = Integer.toString(messageLimit);
        rabbitMQClient.getChannel().basicPublish("", REGISTRATION_QUEUE, new AMQP.BasicProperties.Builder()
                .replyTo(queueName)
                .build(), message.getBytes(StandardCharsets.UTF_8));

        System.out.println(" [*] Límite de mensajes (" + messageLimit + ") enviado al servidor para la cola: " + queueName);
        return queueName;
    }

    // TODO Funcion que extiende la conexion con el servidor reabriendo el canal
    public void reRegisterClient(int messageLimit) throws Exception {
        if (rabbitMQClient.getChannel().getConnection().isOpen() && rabbitMQClient.getChannel().isOpen()) {
            rabbitMQClient.getChannel().close();
        }
        rabbitMQClient.getChannel().queueDelete(REGISTRATION_QUEUE);

        registerClient(messageLimit);
    }
}
