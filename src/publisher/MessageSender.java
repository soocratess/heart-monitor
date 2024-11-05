package publisher;

import com.rabbitmq.client.Channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageSender {
    private static final String GREEN = "\033[32m";
    private static final String RESET = "\u001B[0m";
    private final Channel publishChannel; // Canal exclusivo para enviar mensajes
    private final BufferedReader reader;
    private final ClientRegistry clientRegistry;
    private final ScheduledExecutorService scheduler;

    public MessageSender(Channel publishChannel, BufferedReader reader, ClientRegistry clientRegistry, ScheduledExecutorService scheduler) {
        this.publishChannel = publishChannel;
        this.reader = reader;
        this.clientRegistry = clientRegistry;
        this.scheduler = scheduler;
    }

    public void startSendingMessages() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                String message = reader.readLine();
                if (message != null) {
                    // Enviar el mensaje utilizando el canal de publicación
                    publishChannel.basicPublish("logs", "", null, message.getBytes(StandardCharsets.UTF_8));
                    System.out.println(" [x] Sent '" + GREEN + message + RESET + "'");

                    // Manejar las colas de los clientes sin afectar el canal de publicación
                    clientRegistry.handleClientQueues();
                } else {
                    System.out.println("No more lines to read. Shutting down...");
                    scheduler.shutdown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
