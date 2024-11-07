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

public class MessageSender {
    private static final String GREEN = "\033[32m";
    private static final String RESET = "\u001B[0m";
    private final Channel publishChannel; // Exclusive channel for sending messages
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
     * Starts sending messages to connected clients.
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
                        // Create headers with the line number
                        Map<String, Object> headers = new HashMap<>();
                        headers.put("line-number", lineNumber);

                        // Set properties with headers
                        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                                .headers(headers)
                                .build();

                        // Send the message with properties including the line number
                        publishChannel.basicPublish("logs", "", properties, message.getBytes(StandardCharsets.UTF_8));
                        System.out.println(" [x] Sent '" + GREEN + message + RESET + "' with line number: " + lineNumber);

                        // Handle client queues without affecting the publish channel
                        clientRegistry.handleClientQueues();
                    } else if (message == null) {
                        System.out.println("No more lines to read. Shutting down...");
                        timer.cancel(); // Stops the timer when there are no more messages
                    } else {
                        System.out.println("No clients connected. Line number " + lineNumber);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000); // Executes every 1000 ms = 1 second
    }
}
