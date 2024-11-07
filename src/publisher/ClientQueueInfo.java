package publisher;

/**
 * This class is used to store the message count and message limit for a client.
 */
public class ClientQueueInfo {
    private int messageCount;
    private final int messageLimit;

    public ClientQueueInfo(int messageLimit) {
        this.messageCount = 0;
        this.messageLimit = messageLimit;
    }

    /**
     * Increments the message count for the client.
     */
    public void incrementMessageCount() {
        messageCount++;
    }

    /**
     * Checks if the client has reached the message limit.
     * @return true if the client has reached the message limit, false otherwise
     */
    public boolean hasReachedLimit() {
        return messageCount >= messageLimit;
    }
}
