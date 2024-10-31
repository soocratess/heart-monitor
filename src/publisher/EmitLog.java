package publisher;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class EmitLog {

    private static final String EXCHANGE_NAME = "logs";
    private static final String REGISTRATION_QUEUE = "registration_queue"; // Cola para recibir registros
    private static final String FILE_PATH = "src/data/rr1.txt";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
             Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // Declarar el exchange y la cola de registro
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            channel.queueDeclare(REGISTRATION_QUEUE, false, false, false, null);

            // Crear el registro de clientes y configurar el listener
            ClientRegistry clientRegistry = new ClientRegistry(channel);
            clientRegistry.setupRegistrationListener();

            // Crear el programador de envío de mensajes
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            MessageSender messageSender = new MessageSender(channel, reader, clientRegistry, scheduler);
            messageSender.startSendingMessages();

            // Configurar el shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    scheduler.shutdown();
                    channel.close();
                    connection.close();
                    System.out.println("Conexión y canal cerrados.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
