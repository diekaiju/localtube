package org.schabi.newpipe.localserver;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteWebSocketServer extends WebSocketServer {
    
    private final Set<WebSocket> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public RemoteWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        LocalHttpServer.log("WebSocket client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        LocalHttpServer.log("WebSocket client disconnected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Forward the message to LocalHttpServer pending commands queue
        LocalHttpServer.addPendingCommand(message);
        
        // Broadcast immediately to other connected clients (like the TV browser)
        broadcastCommand(message, conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LocalHttpServer.log("WebSocket error: " + ex.getMessage());
        if (conn != null) {
            connections.remove(conn);
        }
    }

    @Override
    public void onStart() {
        LocalHttpServer.log("WebSocket server started on port " + getPort());
    }
    
    public void broadcastCommand(String command) {
        broadcastCommand(command, null);
    }
    
    public void broadcastCommand(String command, WebSocket excludeConn) {
        for (WebSocket conn : connections) {
            if (conn != excludeConn && conn.isOpen()) {
                try {
                    conn.send(command);
                } catch (Exception e) {
                    LocalHttpServer.log("Failed to send WS command: " + e.getMessage());
                }
            }
        }
    }
}
