package ee.rmit.xrd.utils;

import ee.rmit.xrd.concurrency.SimpleDateFormatThreadSafe;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;

public final class LoggerUtils {
    private static boolean useLogger = true;
    public static final SimpleDateFormatThreadSafe TIMESTAMP = new SimpleDateFormatThreadSafe("dd.MM.yyyy HH:mm:ss.S");

    private LoggerUtils() {
    }

    public static void loggerOn() {
        useLogger = true;
    }

    public static void loggerOff() {
        useLogger = false;
    }

    public static boolean isUseLogger() {
        return useLogger;
    }

    public static void logInfo(String message) {
        if (useLogger) {
            System.out.printf("%s - INFO - %s%n", getTimestamp(), message);
        }
    }

    public static void logWarning(String message) {
        if (useLogger) {
            System.out.printf("%s - WARN - %s%n", getTimestamp(), message);
        }
    }

    public static void logError(String message) {
        if (useLogger) {
            System.out.printf("%s - ERROR - %s%n", getTimestamp(), message);
        }
    }

    public static void logError(String message, Throwable e) {
        if (useLogger) {
            if (e != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                e.printStackTrace(ps);
                message = (message == null) ? baos.toString() : message + "\n" + baos.toString();
            }
            System.out.printf("%s - ERROR - %s%n", getTimestamp(), message);
        }
    }

    public static void logStopWatch(String message, long start) {
        if (useLogger) {
            System.out.printf("%s - INFO - %s. Run time %s sec%n"
                    , getTimestamp(), message, ((double) (System.currentTimeMillis() - start) / 1000));
        }
    }

    public static String getTimestamp() {
        return TIMESTAMP.format(new Date());
    }
}
