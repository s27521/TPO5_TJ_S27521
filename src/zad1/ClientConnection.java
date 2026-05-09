package zad1;

import java.io.PrintWriter;

public class ClientConnection {
    public String id;
    public PrintWriter writer;

    public ClientConnection(String id, PrintWriter writer) {
        this.id = id;
        this.writer = writer;
    }
}
