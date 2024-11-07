package publisher;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.*;

public class Server {

    private static final String EXCHANGE_NAME = "logs";
    private static final int THREAD_POOL_SIZE = 4;

    /**
     * Main method that starts the server.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Set up RabbitMQ connection and channels
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel connectionChannel = connection.createChannel();
        Channel publishChannel = connection.createChannel();

        // Declare the exchange
        publishChannel.exchangeDeclare(EXCHANGE_NAME, "fanout");

        // Create the file reader outside the scheduled thread
        BufferedReader reader = new BufferedReader(new FileReader("src/data/rr1.txt"));

        // Create an instance of ClientRegistry to manage clients
        ClientRegistry clientRegistry = new ClientRegistry(connectionChannel, publishChannel);

        // Start managing client connections
        try {
            clientRegistry.setupRegistrationListener();  // Run the connection listener
            System.out.println("Connection listener started.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create and configure MessageSender to send messages every second
        MessageSender messageSender = new MessageSender(publishChannel, reader, clientRegistry);
        messageSender.startSendingMessages();

        // Set up a shutdown hook to release resources when the server stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (connectionChannel.isOpen()) connectionChannel.close();
                if (publishChannel.isOpen()) publishChannel.close();
                if (connection.isOpen()) connection.close();
                System.out.println("Connection and channels closed.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }
}
