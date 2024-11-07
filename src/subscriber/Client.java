package subscriber;

import gui.Chart;
import gui.ChartDisplay;

import java.util.Scanner;

public class Client {

    /**
     * Main method of the application
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            // Request server IP
            System.out.print("Enter the server IP: ");
            String serverIp = scanner.nextLine();

            // Request message limit from the user
            System.out.print("Enter the message limit to receive: ");
            int messageLimit = scanner.nextInt();

            // Basic input validation
            if (serverIp.isEmpty() || messageLimit <= 0) {
                System.out.println("Error: Server IP and message limit must be valid.");
                return;
            }

            // Set up GUI
            Chart chart = setupGUI(messageLimit);

            // Execute the "start" method directly from the "Client" class
            Client.start(chart, serverIp, messageLimit);
        }
    }

    /**
     * Configures the application's GUI
     * @param messageLimit Message limit to receive
     * @return Configured "Chart" instance
     */
    private static Chart setupGUI(int messageLimit) {
        Chart chart = new Chart(messageLimit);
        ChartDisplay chartDisplay = new ChartDisplay(chart);

        // Start the GUI thread
        Thread chartThread = new Thread(chartDisplay);
        chartThread.start();

        return chart;
    }

    /**
     * Starts the message subscription process
     * @param chart "Chart" instance for displaying data
     * @param serverIp RabbitMQ server IP
     * @param messageLimit Message limit to receive
     */
    public static void start(Chart chart, String serverIp, int messageLimit) {
        try {
            // Set up RabbitMQ connection and channel
            RabbitMQClient rabbitMQClient = new RabbitMQClient(serverIp);
            rabbitMQClient.setupExchange();

            // Register the message limit on the server
            ClientRegistrer clientRegistrer = new ClientRegistrer(rabbitMQClient);
            String queueName = clientRegistrer.registerClient(messageLimit);
            chart.setClientRegistrer(clientRegistrer);

            // Start message subscription
            MessageSubscriber subscriber = new MessageSubscriber(rabbitMQClient, chart);
            subscriber.startSubscription(queueName, messageLimit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
