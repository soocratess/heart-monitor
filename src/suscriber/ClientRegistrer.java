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

    /**
     * Registra un cliente en el servidor con un límite de mensajes
     * @param messageLimit Límite de mensajes a recibir
     * @return Nombre de la cola registrada
     * @throws Exception
     */
    public String registerClient(int messageLimit) throws Exception {
        // Nombre de la cola dinámico basado en el tiempo en milisegundos
        String queueName = "rabbit.client-" + System.currentTimeMillis();
        rabbitMQClient.getChannel().queueDeclare(queueName, false, false, false, null);
        rabbitMQClient.getChannel().queueBind(queueName, RabbitMQClient.getMessageQueue(), "");
        rabbitMQClient.setQueueName(queueName);

        // Enviar el límite de mensajes al servidor para registrarlo
        String message = Integer.toString(messageLimit);
        rabbitMQClient.getChannel().basicPublish("", REGISTRATION_QUEUE, new AMQP.BasicProperties.Builder()
                .replyTo(queueName)
                .build(), message.getBytes(StandardCharsets.UTF_8));

        System.out.println(" [*] Límite de mensajes (" + messageLimit + ") enviado al servidor para la cola: " + queueName);
        return queueName;
    }

    /**
     * Re-registra un cliente en el servidor con un nuevo límite de mensajes
     * @param messageLimit Nuevo límite de mensajes a recibir
     * @return Nombre de la nueva cola registrada
     * @throws Exception
     */
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
