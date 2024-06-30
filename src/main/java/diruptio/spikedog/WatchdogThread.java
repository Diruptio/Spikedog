package diruptio.spikedog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WatchdogThread extends Thread {
    public static final long MAX_SERVE_TIME = 10000;
    private static final Map<ServeThread, Long> serveThreads = new HashMap<>();

    @Override
    public void run() {
        while (true) {
            try {
                long now = System.currentTimeMillis();
                for (ServeThread thread : new ArrayList<>(serveThreads.keySet())) {
                    if (thread.isInterrupted() || !thread.isAlive()) {
                        serveThreads.remove(thread);
                    } else if (now > serveThreads.get(thread) + MAX_SERVE_TIME) {
                        serveThreads.remove(thread);
                        thread.getClient().close();
                        thread.interrupt();
                    }
                }
                Thread.sleep(100);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void guard(ServeThread serveThread) {
        serveThreads.put(serveThread, System.currentTimeMillis());
    }
}
