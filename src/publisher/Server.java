package publisher;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.*;

public class Server {

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

        // Crear el lector del archivo fuera del hilo programado
        BufferedReader reader = new BufferedReader(new FileReader("src/data/rr1.txt"));

        // Crear una instancia de ClientRegistry para la gestión de clientes
        ClientRegistry clientRegistry = new ClientRegistry(connectionChannel, publishChannel);

        // Iniciar la gestión de conexiones de clientes
            try {
                clientRegistry.setupRegistrationListener();  // Ejecuta el listener de conexiones
                System.out.println("Listener de conexiones iniciado.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        // Crear y configurar MessageSender para enviar mensajes cada segundo
        MessageSender messageSender = new MessageSender(publishChannel, reader, clientRegistry);
        messageSender.startSendingMessages();

        // Configurar el shutdown hook para liberar recursos cuando el servidor se detenga
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
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
