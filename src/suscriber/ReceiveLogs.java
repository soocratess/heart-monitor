package suscriber;

import gui.Chart;

import java.util.Scanner;

public class ReceiveLogs implements Runnable {
    private final Chart chart;

    public ReceiveLogs(Chart chart) {
        this.chart = chart;
    }

    @Override
    public void run() {
        try {
            Scanner scanner = new Scanner(System.in);

            // Configurar conexión y canal de RabbitMQ
            RabbitMQClient rabbitMQClient = new RabbitMQClient("localhost");
            rabbitMQClient.setupExchange();

            // Solicitar límite de mensajes al usuario
            int messageLimit = getMessageLimit(scanner);

            // Registrar el límite de mensajes en el servidor
            ClientRegistrer clientRegistrar = new ClientRegistrer(rabbitMQClient.getChannel());
            String queueName = clientRegistrar.registerClient(messageLimit);

            // Iniciar la suscripción a mensajes
            MessageSubscriber subscriber = new MessageSubscriber(rabbitMQClient.getConnection(), rabbitMQClient.getChannel(), chart);
            subscriber.startSubscription(queueName, messageLimit);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getMessageLimit(Scanner scanner) {
        System.out.print("Ingrese el límite de mensajes a recibir: ");
        int messageLimit = scanner.nextInt();
        scanner.nextLine();
        return messageLimit;
    }
}
