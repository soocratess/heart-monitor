package publisher;

import com.rabbitmq.client.*;

import java.io.IOException;

public class ConnectionManager {
    private static final String REGISTRATION_QUEUE = "registration_queue";
    private final Channel connectionChannel;
    private final Channel publishChannel;

    public ConnectionManager(Channel connectionChannel, Channel publishChannel) {
        this.connectionChannel = connectionChannel;
        this.publishChannel = publishChannel;
    }

    /**
     * Configura el listener para gestionar las conexiones de los clientes.
     */
    public void startListeningForConnections() throws IOException {

        // Declarar la cola de registro si aún no existe
        connectionChannel.queueDeclare(REGISTRATION_QUEUE, false, false, false, null);

        // Callback para gestionar las conexiones de los clientes
        DeliverCallback registerCallback = (consumerTag, delivery) -> {
            String queueName = delivery.getProperties().getReplyTo();
            System.out.println("Cliente registrado en la cola: " + queueName);
        };

        // Consumir mensajes en la cola de registro
        connectionChannel.basicConsume(REGISTRATION_QUEUE, true, registerCallback, consumerTag -> {});
    }
}
