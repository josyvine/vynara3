package com.example.utils;

import android.os.Handler;
import android.os.Looper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class VynaraLogger {

    public enum LogTag {
        SYSTEM, GEMINI, AI, KNOWLEDGE, TOOL_MANIFEST, VALIDATOR, MAPPER, TASK, EXECUTION, GENERATOR, MATERIAL, VALIDATION, CLOUD, BLENDER
    }

    public enum LogLevel {
        INFO, WARNING, ERROR
    }

    public static class LogEntry {
        private final long timestampMs;
        private final LogTag tag;
        private final LogLevel level;
        private final String message;
        private final String threadName;

        public LogEntry(LogTag tag, LogLevel level, String message) {
            this.timestampMs = System.currentTimeMillis();
            this.tag = tag != null ? tag : LogTag.SYSTEM;
            this.level = level != null ? level : LogLevel.INFO;
            this.message = message != null ? message : "";
            this.threadName = Thread.currentThread().getName();
        }

        public long getTimestampMs() { return timestampMs; }
        public LogTag getTag() { return tag; }
        public LogLevel getLevel() { return level; }
        public String getMessage() { return message; }
        public String getThreadName() { return threadName; }

        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestampMs));
        }

        @Override
        public String toString() {
            return getFormattedTime() + " " + tag.name() + " " + message;
        }
    }

    public interface LogListener {
        void onLogAdded(LogEntry entry);
        void onLogsCleared();
    }

    private static final int MAX_LOG_CAPACITY = 2000;
    private static final List<LogEntry> logBuffer = new ArrayList<>();
    private static final List<LogListener> listeners = new CopyOnWriteArrayList<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private VynaraLogger() {}

    // Dynamic logging helper methods mapped to your architectural trace requirements
    public static void system(String msg) { log(LogTag.SYSTEM, LogLevel.INFO, msg); }
    public static void gemini(String msg) { log(LogTag.GEMINI, LogLevel.INFO, msg); }
    public static void ai(String msg) { log(LogTag.AI, LogLevel.INFO, msg); }
    public static void knowledge(String msg) { log(LogTag.KNOWLEDGE, LogLevel.INFO, msg); }
    public static void toolManifest(String msg) { log(LogTag.TOOL_MANIFEST, LogLevel.INFO, msg); }
    
    public static void validator(LogLevel level, String msg) { log(LogTag.VALIDATOR, level, msg); }
    public static void mapper(String msg) { log(LogTag.MAPPER, LogLevel.INFO, msg); }
    public static void task(String msg) { log(LogTag.TASK, LogLevel.INFO, msg); }
    public static void execution(String msg) { log(LogTag.EXECUTION, LogLevel.INFO, msg); }
    public static void generator(String msg) { log(LogTag.GENERATOR, LogLevel.INFO, msg); }
    public static void material(String msg) { log(LogTag.MATERIAL, LogLevel.INFO, msg); }
    
    public static void validation(LogLevel level, String msg) { log(LogTag.VALIDATION, level, msg); }
    public static void cloud(String msg) { log(LogTag.CLOUD, LogLevel.INFO, msg); }
    public static void cloud(LogLevel level, String msg) { log(LogTag.CLOUD, level, msg); }

    // Dedicated Blender internal worker logging methods
    public static void blender(String msg) { log(LogTag.BLENDER, LogLevel.INFO, msg); }
    public static void blender(LogLevel level, String msg) { log(LogTag.BLENDER, level, msg); }
    public static void blenderError(String msg) { log(LogTag.BLENDER, LogLevel.ERROR, msg); }

    // General purpose warning & error logging methods
    public static void w(String msg) {
        log(LogTag.SYSTEM, LogLevel.WARNING, msg);
    }

    public static void e(String msg) {
        log(LogTag.SYSTEM, LogLevel.ERROR, msg);
    }

    public static void e(String msg, Throwable throwable) {
        if (throwable != null) {
            log(LogTag.SYSTEM, LogLevel.ERROR, msg + " | Exception: " + throwable.getLocalizedMessage());
        } else {
            log(LogTag.SYSTEM, LogLevel.ERROR, msg);
        }
    }

    private static synchronized void log(LogTag tag, LogLevel level, String message) {
        LogEntry entry = new LogEntry(tag, level, message);

        if (logBuffer.size() >= MAX_LOG_CAPACITY) {
            logBuffer.remove(0); // Prune oldest entry to preserve memory bounds
        }
        logBuffer.add(entry);

        // Safe main-thread dispatching for active listeners
        mainHandler.post(() -> {
            for (LogListener listener : listeners) {
                try {
                    listener.onLogAdded(entry);
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * Compiles all active logs into a single formatted string for copying to clipboard.
     */
    public static synchronized String getAllLogsAsString() {
        StringBuilder sb = new StringBuilder();
        for (LogEntry entry : logBuffer) {
            if (entry != null) {
                sb.append(entry.getFormattedTime())
                  .append(" [").append(entry.getTag().name()).append("] ");
                if (entry.getLevel() == LogLevel.ERROR) {
                    sb.append("[ERROR] ");
                } else if (entry.getLevel() == LogLevel.WARNING) {
                    sb.append("[WARN] ");
                }
                sb.append(entry.getMessage()).append("\n");
            }
        }
        return sb.toString();
    }

    public static synchronized List<LogEntry> getCopyOfLogs() {
        return new ArrayList<>(logBuffer);
    }

    public static synchronized void clear() {
        logBuffer.clear();
        mainHandler.post(() -> {
            for (LogListener listener : listeners) {
                try {
                    listener.onLogsCleared();
                } catch (Exception ignored) {}
            }
        });
    }

    public static void registerListener(LogListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void unregisterListener(LogListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
}