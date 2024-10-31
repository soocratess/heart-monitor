package publisher;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.HashMap;

public class ClientRegistry {
    private static final String REGISTRATION_QUEUE = "registration_queue";
    private final Channel channel;
    private final HashMap<String, ClientQueueInfo> queueInfoMap;

    public ClientRegistry(Channel channel) {
        this.channel = channel;
        this.queueInfoMap = new HashMap<>();
    }

    public void setupRegistrationListener() throws IOException {
        DeliverCallback registerCallback = (consumerTag, delivery) -> {
            String queueName = delivery.getProperties().getReplyTo();
            int messageLimit = Integer.parseInt(new String(delivery.getBody(), "UTF-8"));
            addClientQueue(queueName, messageLimit);
        };
        channel.basicConsume(REGISTRATION_QUEUE, true, registerCallback, consumerTag -> {});
    }

    public void addClientQueue(String queueName, int messageLimit) {
        queueInfoMap.put(queueName, new ClientQueueInfo(messageLimit));
        System.out.println("Cliente conectado con cola: " + queueName + " y límite de mensajes: " + messageLimit);
    }

    public void handleClientQueues(Channel channel) throws IOException {
        var iterator = queueInfoMap.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            String queueName = entry.getKey();
            ClientQueueInfo queueInfo = entry.getValue();

            queueInfo.incrementMessageCount();
            if (queueInfo.hasReachedLimit()) {
                channel.queueDelete(queueName);
                iterator.remove();
                System.out.println("Cola " + queueName + " eliminada después de alcanzar el límite de mensajes.");
            }
        }
    }
}
