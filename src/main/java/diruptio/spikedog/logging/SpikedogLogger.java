package diruptio.spikedog.logging;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class SpikedogLogger extends Logger {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public SpikedogLogger() {
        super("Spikedog", null);
        setLevel(Level.OFF);
    }

    @Override
    public void log(LogRecord record) {
        Throwable thrown = record.getThrown();
        PrintStream out = thrown == null ? System.out : System.err;
        System.out.printf(
                "[%s] [%s/%s] %s\n",
                dateFormat.format(new Date()),
                record.getLoggerName(),
                record.getLevel(),
                record.getMessage());
        if (thrown != null) {
            thrown.printStackTrace(out);
        }
    }
}
