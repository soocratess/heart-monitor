package publisher;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageSender {
    private static final String GREEN = "\033[32m";
    private static final String RESET = "\u001B[0m";
    private final Channel publishChannel; // Canal exclusivo para enviar mensajes
    private final BufferedReader reader;
    private final ClientRegistry clientRegistry;
    private int lineNumber;

    public MessageSender(Channel publishChannel, BufferedReader reader, ClientRegistry clientRegistry) {
        this.publishChannel = publishChannel;
        this.reader = reader;
        this.clientRegistry = clientRegistry;
        this.lineNumber = 0;
    }

    /**
     * Inicia el envío de mensajes a los clientes conectados.
     */
    public void startSendingMessages() {
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    String message = reader.readLine();
                    lineNumber++;

                    if (message != null && clientRegistry.hasClients()) {
                        // Crear cabeceras con el número de línea
                        Map<String, Object> headers = new HashMap<>();
                        headers.put("line-number", lineNumber);

                        // Establecer propiedades con cabeceras
                        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                                .headers(headers)
                                .build();

                        // Enviar el mensaje con las propiedades que incluyen el número de línea
                        publishChannel.basicPublish("logs", "", properties, message.getBytes(StandardCharsets.UTF_8));
                        System.out.println(" [x] Sent '" + GREEN + message + RESET + "' with line number: " + lineNumber);

                        // Manejar las colas de los clientes sin afectar el canal de publicación
                        clientRegistry.handleClientQueues();
                    } else if (message == null) {
                        System.out.println("No more lines to read. Shutting down...");
                        timer.cancel(); // Detiene el timer cuando no hay más mensajes
                    } else {
                        System.out.println("No clients connected. Line number " + lineNumber);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000); // Ejecuta cada 1000 ms = 1 segundo
    }
}
