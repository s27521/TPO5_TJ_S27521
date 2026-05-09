/**
 *
 *  @author Trinh Jarosław s27521
 *
 */

package zad1;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ChatClient {
    private String host;
    private int port;
    private String id;

    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;

    private StringBuilder chatView = new StringBuilder();

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }

    public void login(){
        try {
            clientSocket = new Socket();
            clientSocket.setSoTimeout(500);
            clientSocket.setReuseAddress(true);
            clientSocket.connect(new InetSocketAddress(host, port), 1000);

            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            writer.println("LOGIN|" + id);

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new Thread(this::listen).start();
    }

    public void logout(){
        writer.println("LOGOUT|" + id);
    }

    public void send(String req){
        writer.println("MSG|" + req);
    }

    public String getChatView(){
        return "\n=== " + id + " chat view\n" + chatView.toString();
    }

    public String getId() {
        return id;
    }

    private void listen() {
        try {
            while (!clientSocket.isClosed()) {
                for (String line; (line = reader.readLine()) != null; ) {
                    chatView.append(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}  
