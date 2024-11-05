package suscriber;

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

    public String reRegisterClient(int messageLimit) throws Exception {
        // Cerrar recursos si están abiertos
        if (rabbitMQClient.getConnection().isOpen() || rabbitMQClient.getChannel().isOpen()) {
            rabbitMQClient.close();
        }

        // Reconfigurar la conexión y registrar la cola nuevamente
        rabbitMQClient.setupConnection();
        String queueName = registerClient(messageLimit);
        System.out.println(" [*] Re-registrado con nueva cola: " + queueName);

        // Retornar el nombre de la nueva cola
        return queueName;
    }


}
