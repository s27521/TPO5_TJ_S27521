/**
 *
 *  @author Trinh Jarosław s27521
 *
 */

package zad1;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    public static int DEFAULT_BACKLOG = 500;

    private int port;
    private ExecutorService reqHandlerService;
    private Thread serverThread;

    private Map<Socket, ClientConnection> clients = new ConcurrentHashMap<>();
    private StringBuffer log = new StringBuffer();

    public ChatServer(int port) {
        this.port = port;
    }

    public void startServer(){
        startServer(DEFAULT_BACKLOG);
    }

    public void startServer(int backlog) {
        ServerSocket ss;
        try {
            ss = new ServerSocket(port, backlog);
            reqHandlerService = Executors.newVirtualThreadPerTaskExecutor();
            serviceConnections(ss);
        } catch (Exception exc) {
            System.err.println("Failed to create ServerSocket: " + exc);
        }
    }

    public void stopServer() {
        serverThread.interrupt();
        try {
            reqHandlerService.shutdown();
            reqHandlerService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        reqHandlerService.shutdownNow();
        String time = LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        log.append(time).append(" ").append("ChatServer: chat closed").append("\n");
    }

    public String getServerLog() {
        return log.toString();
    }

    private void serviceConnections(ServerSocket server) {
        System.out.println("Server started");
        Runnable stask = () -> {
            try (server) {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        Socket client = server.accept();
                        reqHandlerService.execute( () -> handleRequest(client) );
                    } catch (SocketException exc) {
                        if (Thread.currentThread().isInterrupted()) System.out.println("Server stopped");
                        else throw exc;
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        };
        serverThread = Thread.startVirtualThread(stask);
    }

    private void handleRequest(Socket client) {
        try {
            client.setSoTimeout(3000);
            client.setTcpNoDelay(true);

            try (client;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true)
            ) {
                for (String line; (line = reader.readLine()) != null;) {
                    String time = LocalTime.now()
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                    String[] req = line.split("\\|", 2);
                    String cmd = req[0];

                    if (cmd.equalsIgnoreCase("LOGOUT")) {
                        String id = clients.get(client).id;
                        broadcast(id + " logged out", time);
                        clients.remove(client);
                    } else if (cmd.equals("LOGIN")) {
                        String id = req[1];
                        clients.put(client, new ClientConnection(id, writer));
                        broadcast(id + " logged in", time);
                    } else if (cmd.equals("MSG")) {
                        String id = clients.get(client).id;
                        String msg = req[1];
                        broadcast(id + ": " + msg, time);
                    }
                }
            } catch (IOException ignored) {}

        } catch (SocketException exc) {
            System.out.println("Handle request timeout: " + exc);
        }
    }

    private void broadcast(String message, String time) {
        String logLine = time + " " + message + "\n";
        log.append(logLine);

        clients.values().forEach(client -> client.writer.println(message));
    }
}  
