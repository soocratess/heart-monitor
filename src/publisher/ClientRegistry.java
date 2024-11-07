package publisher;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.HashMap;

/**
 * This class is used to register clients and handle their queues.
 */
public class ClientRegistry {
    private static final String REGISTRATION_QUEUE = "registration_queue";
    private final Channel connectionChannel; // Canal exclusivo para el manejo de clientes
    private final Channel publishChannel; // Canal para publicar mensajes
    private final HashMap<String, ClientQueueInfo> queueInfoMap;

    public ClientRegistry(Channel clientChannel, Channel publishChannel) {
        this.connectionChannel = clientChannel;
        this.publishChannel = publishChannel;
        this.queueInfoMap = new HashMap<>();
    }

    public void setupRegistrationListener() throws IOException {
        connectionChannel.queueDeclare(REGISTRATION_QUEUE, true, false, false, null);

        DeliverCallback registerCallback = (consumerTag, delivery) -> {
            String queueName = delivery.getProperties().getReplyTo();
            int messageLimit = Integer.parseInt(new String(delivery.getBody(), "UTF-8"));
            addClientQueue(queueName, messageLimit);
        };

        // Callback que se ejecutará cuando un cliente se desconecte
        CancelCallback disconnectCallback = consumerTag -> {
            System.out.println("Cliente desconectado con consumerTag: " + consumerTag);
            // Buscar y eliminar la cola del cliente del mapa usando el consumerTag
            queueInfoMap.entrySet().removeIf(entry -> entry.getKey().equals(consumerTag));
        };

        connectionChannel.basicConsume(REGISTRATION_QUEUE, true, registerCallback, consumerTag -> {});
    }

    public void addClientQueue(String queueName, int messageLimit) {
        queueInfoMap.put(queueName, new ClientQueueInfo(messageLimit));
        System.out.println("Cliente conectado con cola: " + queueName + " y límite de mensajes: " + messageLimit);
    }

    public void handleClientQueues() throws IOException {
        var iterator = queueInfoMap.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            String queueName = entry.getKey();
            ClientQueueInfo queueInfo = entry.getValue();

            queueInfo.incrementMessageCount();
            if (queueInfo.hasReachedLimit()) {
                publishChannel.queueDelete(queueName); // Eliminar solo la cola del cliente
                iterator.remove();
                System.out.println("Cola " + queueName + " eliminada .");
            }
        }
    }

    public boolean hasClients() {
        return !queueInfoMap.isEmpty();
    }

}
