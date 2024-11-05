package publisher;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.HashMap;

/**
 * This class is used to register clients and handle their queues.
 */
public class ClientRegistry {
    private static final String REGISTRATION_QUEUE = "registration_queue";
    private final Channel clientChannel; // Canal exclusivo para el manejo de clientes
    private final HashMap<String, ClientQueueInfo> queueInfoMap;

    public ClientRegistry(Channel clientChannel) {
        this.clientChannel = clientChannel;
        this.queueInfoMap = new HashMap<>();
    }

    public void setupRegistrationListener() throws IOException {
        DeliverCallback registerCallback = (consumerTag, delivery) -> {
            String queueName = delivery.getProperties().getReplyTo();
            int messageLimit = Integer.parseInt(new String(delivery.getBody(), "UTF-8"));
            addClientQueue(queueName, messageLimit);
        };
        clientChannel.basicConsume(REGISTRATION_QUEUE, true, registerCallback, consumerTag -> {});
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
                clientChannel.queueDelete(queueName); // Eliminar solo la cola del cliente
                iterator.remove();
                System.out.println("Cola " + queueName + " eliminada .");
            }
        }
    }

}
