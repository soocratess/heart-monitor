package suscriber;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;

public class ClientRegistrer {
    private static final String REGISTRATION_QUEUE = "registration_queue";
    private final Channel channel;

    public ClientRegistrer(Channel channel) {
        this.channel = channel;
    }

    public String registerClient(int messageLimit) throws Exception {
        String queueName = channel.queueDeclare("", false, false, true, null).getQueue();
        channel.queueBind(queueName, RabbitMQClient.getExchangeName(), "");

        // Enviar el límite de mensajes al servidor para registrarlo
        String message = Integer.toString(messageLimit);
        channel.basicPublish("", REGISTRATION_QUEUE, new AMQP.BasicProperties.Builder()
                .replyTo(queueName)
                .build(), message.getBytes(StandardCharsets.UTF_8));

        System.out.println(" [*] Límite de mensajes (" + messageLimit + ") enviado al servidor para la cola: " + queueName);
        return queueName;
    }
}
