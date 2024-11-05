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

        // Solicitar ip del servidor
        System.out.print("Ingrese la ip del servidor: ");
        String serverIp = scanner.nextLine();

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
        receiveLogs.start(chart, serverIp, messageLimit);

        // Cerrar el scanner
        scanner.close();
    }

    public void start(Chart chart, String serverIp,  int messageLimit) {
        try {

            // Configurar conexión y canal de RabbitMQ
            RabbitMQClient rabbitMQClient = new RabbitMQClient(serverIp);
            rabbitMQClient.setupExchange();

            // Registrar el límite de mensajes en el servidor
            ClientRegistrer clientRegistrer = new ClientRegistrer(rabbitMQClient);
            String queueName = clientRegistrer.registerClient(messageLimit);
            chart.setClientRegistrer(clientRegistrer);

            // Iniciar la suscripción a mensajes
            MessageSubscriber subscriber = new MessageSubscriber(rabbitMQClient, chart);
            subscriber.startSubscription(queueName, messageLimit);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
