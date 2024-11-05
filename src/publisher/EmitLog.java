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

        BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
        Connection connection = factory.newConnection();
        Channel publishChannel = connection.createChannel(); // Canal para publicar mensajes
        Channel clientManagementChannel = connection.createChannel(); // Canal para gestionar clientes

        try {
            // Declarar el exchange y la cola de registro en el canal de publicación
            publishChannel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            clientManagementChannel.queueDeclare(REGISTRATION_QUEUE, false, false, false, null);

            // Crear el registro de clientes y configurar el listener en el canal de gestión
            ClientRegistry clientRegistry = new ClientRegistry(clientManagementChannel);
            clientRegistry.setupRegistrationListener();

            // Crear el programador de envío de mensajes usando el canal de publicación
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            MessageSender messageSender = new MessageSender(publishChannel, reader, clientRegistry, scheduler);
            messageSender.startSendingMessages();

            // Configurar el shutdown hook para limpiar recursos
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (scheduler != null) {
                        scheduler.shutdown();
                    }
                    if (publishChannel != null && publishChannel.isOpen()) {
                        publishChannel.close();
                    }
                    if (clientManagementChannel != null && clientManagementChannel.isOpen()) {
                        clientManagementChannel.close();
                    }
                    if (connection != null && connection.isOpen()) {
                        connection.close();
                    }
                    System.out.println("Conexión y canales cerrados.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
