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

    private Map<Socket, String> clients = new ConcurrentHashMap<>();
    private List<ServerLog> logs = new ArrayList<>();

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
            String time = LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            logs.add(new ServerLog(time,"ChatServer: chat closed"));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        reqHandlerService.shutdownNow();
    }

    public String getServerLog() {
        return String.join("\n", logs.stream().map(ServerLog::toString).toList()) + "\n";
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
            client.setTcpNoDelay(false);

            try (client;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true)
            ) {
                int i = 0;
                for (String line; (line = reader.readLine()) != null;) {
                    String time = LocalTime.now()
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss.nnn"));
                    String[] req = line.split("\\|", 2);
                    String cmd = req[0];

                    if (cmd.equalsIgnoreCase("LOGOUT")) {
                        String id = clients.get(client);
                        addLog(id + " logged out", time);
                        clients.remove(client);
                    } else if (cmd.equals("LOGIN")) {
                        String id = req[1];
                        clients.put(client, id);
                        i = addLog(id + " logged in", time);
                    } else if (cmd.equals("MSG")) {
                        String id = clients.get(client);
                        String msg = req[1];
                        addLog(id + ": " + msg, time);
                    }
                    writer.println(logs.get(i).message);
                    i++;

                    if (serverThread.isInterrupted())
                        break;
                }
                clients.remove(client);
                while (!clients.isEmpty()) {}
                for (; i < logs.size(); i++) {
                    writer.println(logs.get(i).message);
                }
                writer.println("ChatServer: chat closed");
            } catch (IOException ignored) {}

        } catch (SocketException exc) {
            System.out.println("Handle request timeout: " + exc);
        }
    }

    private synchronized int addLog(String message, String time) {
        String timer = LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.nnn"));
        logs.add(new ServerLog(timer, message));
        return logs.size()-1;
    }
}  
