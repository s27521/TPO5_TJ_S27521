package zad1;


import java.io.PrintWriter;

public class ClientConnection {
    private String id;
    private PrintWriter writer;
    private int lastSentMessage;

    public ClientConnection(PrintWriter writer) {
        this.writer = writer;
        lastSentMessage = 0;
    }

    public int getLastSentMessage() {
        return lastSentMessage;
    }

    public void setLastSentMessage(int lastSentMessage) {
        this.lastSentMessage = lastSentMessage;
    }

    public void raise() {
        lastSentMessage++;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
