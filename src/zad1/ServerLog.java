package zad1;


public class ServerLog {
    public String time;
    public String message;

    public ServerLog(String time, String message) {
        this.time = time;
        this.message = message;
    }

    @Override
    public String toString() {
        return time + " " + message;
    }
}
