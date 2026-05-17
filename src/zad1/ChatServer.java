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
import java.util.ArrayList;
import java.util.List;
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

    private Object broadcastLock = new Object();
    private ServerSocket ss;

    private Map<Socket, ClientConnection> clients = new ConcurrentHashMap<>();
    private List<String> logs = new ArrayList<>();

    public ChatServer(int port) {
        this.port = port;
    }

    public void startServer(){
        startServer(DEFAULT_BACKLOG);
    }

    public void startServer(int backlog) {
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
            broadcast("ChatServer: chat closed");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        reqHandlerService.shutdownNow();
    }

    public String getServerLog() {
        return String.join("\n", logs) + "\n";
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

    private int lastBroadcastMessage = 0;

    private void handleRequest(Socket socket) {
        try {
            socket.setSoTimeout(3000);
            socket.setTcpNoDelay(false);

            try (socket;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true)
            ) {
                ClientConnection client = new ClientConnection(writer);
                String line;

                while ((line = reader.readLine()) != null) {
                    String[] req = line.split("\\|", 2);
                    String cmd = req[0];

                    if (cmd.equalsIgnoreCase("LOGOUT")) {
                        String id = clients.get(socket).getId();
                        broadcast(id + " logged out");
                        clients.remove(socket);
                    } else if (cmd.equals("LOGIN")) {
                        String id = req[1];
                        client.setId(id);
                        clients.put(socket, client);
                        broadcast(id + " logged in");
                    } else if (cmd.equals("MSG")) {
                        String id = clients.get(socket).getId();
                        String msg = req[1];
                        broadcast(id + ": " + msg);
                    }
                    if (serverThread.isInterrupted())
                        break;
                }
                while (!clients.isEmpty()) {}
            } catch (IOException ignored) {}

        } catch (SocketException exc) {
            System.out.println("Handle request timeout: " + exc);
        }
    }

    private void broadcast(String message) {
        synchronized (broadcastLock) {
            String time = LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss.nnn"));
            logs.add(time + " " + message);
            lastBroadcastMessage++;
            for (ClientConnection value : clients.values()) {
                value.getWriter().println(message);
                value.raise();
            }
            if (message.equalsIgnoreCase("ChatServer: chat closed"))
                clients.clear();
        }
    }
}  
