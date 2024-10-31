package suscriber;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static java.lang.System.exit;

public class ReceiveLogs {

    private static final String EXCHANGE_NAME = "logs";
    private static final String REGISTRATION_QUEUE = "registration_queue";  // Cola de registro en el servidor
    private static final String GREEN = "\033[32m";
    private static final String RESET = "\u001B[0m";

    public static void main(String[] argv) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Configurar conexión y canal de RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        setupExchange(channel);

        // Solicitar límite de mensajes al usuario
        int messageLimit = getMessageLimit(scanner);

        // Crear una cola exclusiva para este cliente y registrar el límite de mensajes en el servidor
        // Crear una cola exclusiva para este cliente y registrar el límite de mensajes en el servidor
        String queueName = channel.queueDeclare("", false, false, true, null).getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        // Enviar el límite de mensajes al servidor para registrarlo
        registerMessageLimit(channel, queueName, messageLimit);

        // Iniciar la suscripción y desconectar al alcanzar el límite de mensajes
        startSubscription(connection, channel, queueName, messageLimit);
    }

    /**
     * Configura el exchange para la comunicación.
     */
    private static void setupExchange(Channel channel) throws Exception {
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        System.out.println(" [*] Conectado al exchange " + EXCHANGE_NAME);
    }

    /**
     * Solicita al usuario el límite de mensajes.
     */
    private static int getMessageLimit(Scanner scanner) {
        System.out.print("Ingrese el límite de mensajes a recibir: ");
        int messageLimit = scanner.nextInt();
        scanner.nextLine();  // Limpiar el buffer del scanner
        return messageLimit;
    }

    /**
     * Envía el límite de mensajes al servidor en la cola de registro.
     */
    private static void registerMessageLimit(Channel channel, String queueName, int messageLimit) throws Exception {
        String message = Integer.toString(messageLimit);

        // Enviar el límite de mensajes a la cola de registro con el nombre de la cola del cliente en `replyTo`
        channel.basicPublish("", REGISTRATION_QUEUE, new AMQP.BasicProperties.Builder()
                .replyTo(queueName)  // Usar el nombre de la cola del cliente en `replyTo`
                .build(), message.getBytes("UTF-8"));

        System.out.println(" [*] Límite de mensajes (" + messageLimit + ") enviado al servidor para la cola: " + queueName);
    }

    /**
     * Inicia la suscripción al canal y se desconecta después de recibir el número de mensajes especificado.
     */
    private static void startSubscription(Connection connection,Channel channel, String queueName, int messageLimit) throws Exception {
        final int[] messageCount = {0};  // Contador de mensajes

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            if (messageCount[0] < messageLimit) {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println(" [x] Received '" + GREEN + message + RESET + "'");
                messageCount[0]++;

                try {
                    // Verificar si se alcanzó el límite de mensajes
                    if (messageCount[0] >= messageLimit) {
                        System.out.println(" [*] Se ha alcanzado el límite de mensajes (" + messageLimit + "). Desconectando...");
                        channel.queueDelete(queueName);  // Eliminar la cola en el servidor
                        channel.close();  // Cerrar el canal
                        connection.close();  // Cerrar la conexión
                        exit(0);  // Salir del programa
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // Iniciar la suscripción
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}
