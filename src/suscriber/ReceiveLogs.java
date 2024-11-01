package suscriber;

import gui.Chart;
import gui.ChartDisplay;

import java.util.Scanner;

public class ReceiveLogs {
    private final Chart chart;

    public ReceiveLogs(Chart chart) {
        this.chart = chart;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Solicitar límite de mensajes al usuario
        System.out.print("Ingrese el límite de mensajes a recibir: ");
        int messageLimit = scanner.nextInt();
        scanner.nextLine();

        // Crear la instancia de la GUI
        Chart chart = new Chart(messageLimit);
        ChartDisplay chartDisplay = new ChartDisplay(chart);

        // Iniciar el hilo de la GUI
        Thread chartThread = new Thread(chartDisplay);
        chartThread.start();

        // Iniciar el proceso de ReceiveLogs
        ReceiveLogs receiveLogs = new ReceiveLogs(chart);
        receiveLogs.start(chart, messageLimit);
    }

    public void start(Chart chart, int messageLimit) {
        try {

            // Configurar conexión y canal de RabbitMQ
            RabbitMQClient rabbitMQClient = new RabbitMQClient("localhost");
            rabbitMQClient.setupExchange();

            // Registrar el límite de mensajes en el servidor
            ClientRegistrer clientRegistrer = new ClientRegistrer(rabbitMQClient.getChannel());
            String queueName = clientRegistrer.registerClient(messageLimit);

            // Iniciar la suscripción a mensajes
            MessageSubscriber subscriber = new MessageSubscriber(rabbitMQClient.getConnection(), rabbitMQClient.getChannel(), chart);
            subscriber.startSubscription(queueName, messageLimit);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
