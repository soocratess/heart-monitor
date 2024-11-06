package suscriber;

import gui.Chart;
import gui.ChartDisplay;

import java.util.Scanner;

public class Client {

    /**
     * Metodo principal de la aplicación
     * @param args Argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            // Solicitar IP del servidor
            System.out.print("Ingrese la IP del servidor: ");
            String serverIp = scanner.nextLine();

            // Solicitar límite de mensajes al usuario
            System.out.print("Ingrese el límite de mensajes a recibir: ");
            int messageLimit = scanner.nextInt();

            // Validación básica de entradas
            if (serverIp.isEmpty() || messageLimit <= 0) {
                System.out.println("Error: IP del servidor y límite de mensajes deben ser válidos.");
                return;
            }

            // Configurar GUI
            Chart chart = setupGUI(messageLimit);

            // Ejecutar el metodo "start" directamente desde la clase "Client"
            Client.start(chart, serverIp, messageLimit);
        }
    }

    /**
     * Configura la GUI de la aplicación
     * @param messageLimit Límite de mensajes a recibir
     * @return Instancia de la clase "Chart" configurada
     */
    private static Chart setupGUI(int messageLimit) {
        Chart chart = new Chart(messageLimit);
        ChartDisplay chartDisplay = new ChartDisplay(chart);

        // Iniciar el hilo de la GUI
        Thread chartThread = new Thread(chartDisplay);
        chartThread.start();

        return chart;
    }

    /**
     * Inicia el proceso de suscripción a mensajes
     * @param chart Instancia de la clase "Chart" para mostrar los datos
     * @param serverIp IP del servidor RabbitMQ
     * @param messageLimit Límite de mensajes a recibir
     */
    public static void start(Chart chart, String serverIp, int messageLimit) {
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
