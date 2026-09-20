package com.arc.logger;

public class Logger {

    public static final int LEVEL_VERBOSE = 0;
    public static final int LEVEL_DEBUG = 1;
    public static final int LEVEL_WARNING = 2;
    public static final int LEVEL_ERROR = 3;
    public static final int LEVEL_PAYWALL = 4;
    public static final int LEVEL_METRICS = 5;

    private static LogHandler logHandler = new DefaultLogHandler();

    public static void init(LogHandler logHandler) {
        Logger.logHandler = logHandler;
    }

    /**
     * Verbose message
     */
    public static void v(String msg, Payload payload) {
        log(LEVEL_VERBOSE, msg, payload);
    }

    /**
     * Debug message
     */
    public static void d(String msg, Payload payload) {
        log(LEVEL_DEBUG, msg, payload);
    }

    public static void d(String msg) {
        log(LEVEL_DEBUG, msg, null);
    }

    /**
     * Warning message
     */
    public static void w(String msg, Payload payload) {
        log(LEVEL_WARNING, msg, payload);
    }

    public static void w(String msg) {
        log(LEVEL_WARNING, msg, null);
    }

    /**
     * Error message
     */
    public static void e(String msg, Payload payload) {
        log(LEVEL_ERROR, msg, payload);
    }

    public static void e(String msg) {
        log(LEVEL_ERROR, msg, null);
    }

    /**
     * Paywall message
     */
    public static void p(String msg, Payload payload) {
        log(LEVEL_PAYWALL, msg, payload);
    }

    /**
     * Metrics message
     */
    public static void m(String msg, Payload payload) {
        log(LEVEL_METRICS, msg, payload);
    }

    public static void log(int level, String msg, Payload payload) {
        logHandler.log(level, msg, payload);
    }

    abstract public static class LogHandler {
        abstract void log(int level, String msg, Payload payload);
    }
}
