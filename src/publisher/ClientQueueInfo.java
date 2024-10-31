package publisher;

public class ClientQueueInfo {
    private int messageCount;
    private final int messageLimit;

    public ClientQueueInfo(int messageLimit) {
        this.messageCount = 0;
        this.messageLimit = messageLimit;
    }

    public void incrementMessageCount() {
        messageCount++;
    }

    public boolean hasReachedLimit() {
        return messageCount >= messageLimit;
    }
}
