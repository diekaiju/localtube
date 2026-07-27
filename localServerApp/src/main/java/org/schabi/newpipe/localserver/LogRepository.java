package org.schabi.newpipe.localserver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogRepository implements LocalHttpServer.LogListener {

    public interface LogListener {
        void onLogAdded(String formattedLine);
        void onLogsCleared();
    }

    private static LogRepository instance;
    private final List<String> htmlLogLines = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private LogListener activeListener;

    public static synchronized LogRepository getInstance() {
        if (instance == null) {
            instance = new LogRepository();
        }
        return instance;
    }

    private LogRepository() {
        LocalHttpServer.setLogListener(this);
    }

    @Override
    public void onLog(String message) {
        String time = dateFormat.format(new Date());
        String formattedLine = formatLogToHtml(time, message);
        synchronized (htmlLogLines) {
            htmlLogLines.add(formattedLine);
            if (htmlLogLines.size() > 500) {
                htmlLogLines.remove(0);
            }
        }
        synchronized (this) {
            if (activeListener != null) {
                activeListener.onLogAdded(formattedLine);
            }
        }
    }

    public List<String> getLogs() {
        synchronized (htmlLogLines) {
            return new ArrayList<>(htmlLogLines);
        }
    }

    public void clear() {
        synchronized (htmlLogLines) {
            htmlLogLines.clear();
        }
        synchronized (this) {
            if (activeListener != null) {
                activeListener.onLogsCleared();
            }
        }
    }

    public synchronized void setActiveListener(LogListener listener) {
        this.activeListener = listener;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    private String formatLogToHtml(String time, String message) {
        String displayMessage = message;
        boolean isTruncated = false;
        if (message.length() > 200 || message.contains("\n")) {
            int newlineIdx = message.indexOf("\n");
            if (newlineIdx > 0 && newlineIdx < 120) {
                displayMessage = message.substring(0, newlineIdx);
            } else {
                displayMessage = message.substring(0, Math.min(message.length(), 120));
            }
            isTruncated = true;
        }

        String escapedMessage = escapeHtml(displayMessage);
        if (isTruncated) {
            escapedMessage += " <font color='#64748B'><b>[Truncated: " + message.length() + " chars]</b></font>";
        }

        String colorTime = "#64748B"; // Slate-400
        String colorMessage = "#E2E8F0"; // Slate-200 (default)

        String lowerMsg = displayMessage.toLowerCase(Locale.US);
        if (lowerMsg.contains("error") || lowerMsg.contains("exception") || lowerMsg.contains("failed")) {
            colorMessage = "#F87171"; // Red-400
        } else if (lowerMsg.contains("started") || lowerMsg.contains("completed")) {
            colorMessage = "#4ADE80"; // Green-400
        } else if (lowerMsg.contains("stopped")) {
            colorMessage = "#FB923C"; // Orange-400
        } else if (lowerMsg.startsWith("request:")) {
            colorMessage = "#E2E8F0";
            if (escapedMessage.contains(" GET ")) {
                escapedMessage = escapedMessage.replace("Request:", "<font color='#F472B6'><b>REQ</b></font>") // Pink-400
                                               .replace(" GET ", " <font color='#4ADE80'><b>GET</b></font> <font color='#38BDF8'>"); // LightBlue-400
                escapedMessage += "</font>";
            } else if (escapedMessage.contains(" POST ")) {
                escapedMessage = escapedMessage.replace("Request:", "<font color='#F472B6'><b>REQ</b></font>")
                                               .replace(" POST ", " <font color='#FB923C'><b>POST</b></font> <font color='#38BDF8'>");
                escapedMessage += "</font>";
            }
        } else if (lowerMsg.contains("proxying stream") || lowerMsg.contains("serving local")) {
            colorMessage = "#C084FC"; // Purple-400
        }

        if (lowerMsg.startsWith("request:")) {
            return "<font color='" + colorTime + "'>[" + time + "]</font> " + escapedMessage + "<br/>";
        } else {
            return "<font color='" + colorTime + "'>[" + time + "]</font> <font color='" + colorMessage + "'>" + escapedMessage + "</font><br/>";
        }
    }
}
