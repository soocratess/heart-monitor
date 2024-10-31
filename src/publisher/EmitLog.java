package publisher;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EmitLog {

    private static final String GREEN = "\033[32m";
    private static final String RESET = "\u001B[0m";
    private static final String EXCHANGE_NAME = "logs";
    private static final String REGISTRATION_QUEUE = "registration_queue";  // Cola para recibir registros
    private static final String FILE_PATH = "src/data/rr1.txt";

    // Clase auxiliar para almacenar el contador de mensajes y el límite por cliente
    static class ClientQueueInfo {
        int messageCount;
        int messageLimit;

        ClientQueueInfo(int messageLimit) {
            this.messageCount = 0;
            this.messageLimit = messageLimit;
        }

        void incrementMessageCount() {
            messageCount++;
        }

        boolean hasReachedLimit() {
            return messageCount >= messageLimit;
        }
    }

    // HashMap para almacenar las colas de clientes con su información de límite
    private static HashMap<String, ClientQueueInfo> queueInfoMap = new HashMap<>();

    public static void main(String[] argv) throws Exception {

        // Configurar conexión y canal de RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // Declarar el exchange y la cola de registro
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            channel.queueDeclare(REGISTRATION_QUEUE, false, false, false, null);

            // Configurar el consumidor para recibir los registros de los clientes
            setupRegistrationListener(channel);

            // Programar la tarea de enviar cada línea del archivo
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduleMessageSending(scheduler, channel, reader);

            // Configurar el shutdown hook
            configureShutdownHook(scheduler, channel, connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Configura el listener para la cola de registro de clientes.
     */
    private static void setupRegistrationListener(Channel channel) throws IOException {
        DeliverCallback registerCallback = (consumerTag, delivery) -> {
            String queueName = delivery.getProperties().getReplyTo();  // Obtener el nombre de la cola del cliente
            int messageLimit = Integer.parseInt(new String(delivery.getBody(), "UTF-8"));  // Parsear el límite de mensajes

            // Registrar el cliente en el HashMap con su límite
            addClientQueue(queueName, messageLimit);
        };
        channel.basicConsume(REGISTRATION_QUEUE, true, registerCallback, consumerTag -> {});
    }

    /**
     * Programa el envío de una línea del archivo cada segundo.
     */
    private static void scheduleMessageSending(ScheduledExecutorService scheduler, Channel channel, BufferedReader reader) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                String message = reader.readLine(); // Leer la siguiente línea del archivo
                if (message != null) {
                    channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes("UTF-8"));
                    System.out.println(" [x] Sent '" + GREEN + message + RESET + "'");

                    // Lógica para verificar y eliminar colas si alcanzan su límite
                    handleClientQueues(channel);

                } else {
                    System.out.println("No more lines to read. Shutting down...");
                    scheduler.shutdown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS); // Ejecutar cada 1 segundo
    }

    /**
     * Función para manejar las colas de los clientes y eliminar las que alcanzan su límite de mensajes.
     */
    private static void handleClientQueues(Channel channel) throws IOException {
        for (String queueName : queueInfoMap.keySet()) {
            ClientQueueInfo queueInfo = queueInfoMap.get(queueName);

            // Incrementar contador de mensajes de la cola
            queueInfo.incrementMessageCount();

            // Verificar si ha alcanzado el límite
            if (queueInfo.hasReachedLimit()) {
                // Eliminar la cola si ha alcanzado su límite de mensajes
                channel.queueDelete(queueName);
                queueInfoMap.remove(queueName);
                System.out.println("Cola " + queueName + " eliminada después de alcanzar el límite de mensajes.");
            }
        }
    }

    /**
     * Configura un shutdown hook para cerrar la conexión, el canal y el scheduler al finalizar el programa.
     */
    private static void configureShutdownHook(ScheduledExecutorService scheduler, Channel channel, Connection connection) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                scheduler.awaitTermination(1, TimeUnit.SECONDS);
                channel.close();
                connection.close();
                System.out.println("Conexión y canal cerrados.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * Método para agregar un cliente y su cola al HashMap con el contador inicializado en 0 y su límite específico.
     */
    public static void addClientQueue(String queueName, int messageLimit) {
        queueInfoMap.put(queueName, new ClientQueueInfo(messageLimit));
        System.out.println("Cliente conectado con cola: " + queueName + " y límite de mensajes: " + messageLimit);
    }
}
