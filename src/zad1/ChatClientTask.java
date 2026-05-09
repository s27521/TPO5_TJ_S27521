/**
 *
 *  @author Trinh Jarosław s27521
 *
 */

package zad1;


import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ChatClientTask extends FutureTask<Void> {
    private ChatClient c;

    private ChatClientTask(ChatClient c, Callable<Void> task) {
        super(task);
        this.c = c;
    }

    public static ChatClientTask create(ChatClient c, List<String> msgs, int wait) {
        Callable<Void> task = () -> {
            try {
                c.login();
                if (wait != 0) Thread.sleep(wait);

                for (String msg : msgs) {
                    c.send(msg);
                    if (wait != 0) Thread.sleep(wait);
                }
                c.logout();
                if (wait != 0) Thread.sleep(wait);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };
        return new ChatClientTask(c, task);
    }

    public ChatClient getClient() {
        return c;
    }
}  
