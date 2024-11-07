# Heart Monitor - Real-Time Publisher and Subscriber

This project implements a real-time monitoring system for a patient's heart rate in a coronary care unit, using the publish-subscribe model with RabbitMQ in Java. The data is read from a file representing a time series of RR intervals (the time in seconds between consecutive heartbeats) and published to subscribed clients in real-time.

## Features

1. **Real-Time Publisher**: The server reads the data file and publishes one line every second. Each message includes the line number in the header.
2. **Subscriber**: Clients can subscribe to receive data for a specified period. Each client graphically displays the received data, showing the last minute of data in real-time.
3. **Subscription Renewal**: Clients can renew their subscription before it expires to continue receiving data, updating the time limit as defined by the user.
4. **Multi-Client Support**: The server can handle multiple clients subscribed simultaneously.
5. **Real-Time Graphical Representation**: Each client displays the data in a graphical interface, showing a real-time visualization of the last minute of received data.

## Project Structure

The project is organized into the following packages:

- **`publisher`**: Contains classes related to the server and data publication.
    - `ClientQueueInfo`: Stores the message limit and the current count for each client.
    - `ClientRegistry`: Manages the registration and handling of client queues.
    - `MessageSender`: Reads data from the file and sends messages to subscribed clients.
    - `Server`: Configures and runs the publishing server.

- **`subscriber`**: Contains classes related to the client and data subscription.
    - `Client`: Main client class that enables subscription and renewal.
    - `ClientRegistrer`: Allows registering or renewing a client's subscription on the server.
    - `MessageSubscriber`: Receives and processes messages from the server.
    - `RabbitMQClient`: Manages the RabbitMQ connection and channel for the client.

- **`gui`**: Contains classes related to the client’s graphical interface.
    - `Chart`: Displays data in a line chart with a graphical user interface (GUI).
    - `ChartDisplay`: Sets up and manages the main display window.

## Requirements

- **Java 8** or higher.
- **RabbitMQ** installed and running.
- Additional dependencies (add to classpath):
    - `amqp-client-5.7.1.jar` (for connecting to RabbitMQ)
    - `jfreechart-1.0.19.jar` (for graphical visualization)

## Project Setup

1. **Install and Configure RabbitMQ**:
    - Ensure that RabbitMQ is installed and running on `localhost` or the IP specified by the client.
    - Create the required `exchange` for the project using the `fanout` configuration.

2. **File Structure**:
    - Place the `rr1.txt` file in the `src/data` directory so the server can read it.

3. **Compilation and Execution**:
    - Compile the project, ensuring all dependencies are in the classpath.

## Running the Project

### Starting the Server

1. Run the `Server` class in the `publisher` package.
2. The server will start reading the `rr1.txt` file and publishing the data to the RabbitMQ `exchange` every second.

### Starting a Client

1. Run the `Client` class in the `subscriber` package.
2. Provide the server IP and the message limit for the subscription.
3. The client will connect to the server, receive real-time data, and display the heart rate in a chart.

### Renewing the Subscription

1. In the client's GUI, enter the new time limit (in seconds) in the text field.
2. Click the "Renew" button to extend the subscription with the new time limit.

## Message Structure

The server sends each message with the following structure:
- **Body**: RR interval (in seconds) between two consecutive heartbeats.
- **Headers**:
    - `line-number`: Line number from the file (used as an identifier for each data point).

## Notes

- Subscription is managed by the server; the client stops receiving messages when its time expires.
- In the chart, data is displayed in real-time, maintaining the last minute of received signals.
- Clients can renew without needing to restart the application.

## Credits

This project was developed as a distributed programming practice using Java RMI and RabbitMQ.
