package publisher;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class EmitLog {

    private static final String EXCHANGE_NAME = "logs";
    private static final int THREAD_POOL_SIZE = 4;

    public static void main(String[] args) throws Exception {
        // Configurar la conexión y los canales de RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel connectionChannel = connection.createChannel();
        Channel publishChannel = connection.createChannel();

        // Declarar el exchange
        publishChannel.exchangeDeclare(EXCHANGE_NAME, "fanout");

        // Crear el pool de hilos con un tamaño fijo
        ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);

        // Crear el lector del archivo fuera del hilo programado
        BufferedReader reader = new BufferedReader(new FileReader("src/data/rr1.txt"));

        // Crear una instancia de ClientRegistry para la gestión de clientes
        ClientRegistry clientRegistry = new ClientRegistry(connectionChannel);

        // Crear una instancia de ConnectionManager para gestionar conexiones de clientes
        ConnectionManager connectionManager = new ConnectionManager(connectionChannel, publishChannel);

        // Iniciar la gestión de conexiones de clientes
        threadPool.scheduleAtFixedRate(() -> {
            try {
                connectionManager.startListeningForConnections();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.SECONDS);

        // Crear y configurar MessageSender para enviar mensajes cada segundo
        MessageSender messageSender = new MessageSender(publishChannel, reader, clientRegistry, threadPool);
        messageSender.startSendingMessages();

        // Configurar el shutdown hook para liberar recursos cuando el servidor se detenga
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                threadPool.shutdown();
                if (connectionChannel.isOpen()) connectionChannel.close();
                if (publishChannel.isOpen()) publishChannel.close();
                if (connection.isOpen()) connection.close();
                System.out.println("Conexión y canales cerrados.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }
}
