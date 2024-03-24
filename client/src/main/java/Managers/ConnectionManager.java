package main.java.Managers;

import main.java.Server.ServerHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private BufferedReader reader;
    private PrintWriter writer;
    private Socket socket;
    public CommunicationManager communicationManager;

    private static final Map<String, ServerHandler> users = new ConcurrentHashMap<>();
    private static final Map<String, String> allPublicKeys = new ConcurrentHashMap<>();

    private String username = "";
    private boolean isLoggedIn = false;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public void setupStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        communicationManager = new CommunicationManager(this, writer);
    }

    public void sendAllPublicKeys(ServerHandler client) {
        getAllPublicKeys().forEach((user, key) -> {
            if (!user.equals(client.initializeManager.getConnectionManager().getUsername())) { // eigen key niet opsturen
                JsonNode publicKeyMessage = objectMapper.createObjectNode()
                        .put("type", "PUBLIC_KEY")
                        .put("username", user)
                        .put("publicKey", key);
                client.initializeManager.getCommunicationManager().sendMessage("PUBLIC_KEY", publicKeyMessage);
            }
        });
    }

    public void broadcastPublicKey(String username, String publicKey) {
        JsonNode publicKeyMessage = objectMapper.createObjectNode()
                .put("type", "PUBLIC_KEY")
                .put("username", username)
                .put("publicKey", publicKey);

        for (ServerHandler client : ConnectionManager.getUsers().values()) {
            if (!client.initializeManager.getConnectionManager().getUsername().equals(username)) {
                client.initializeManager.getCommunicationManager().sendMessage("PUBLIC_KEY", publicKeyMessage);
            }
        }
    }

    public static Map<String, String> getAllPublicKeys() {
        return allPublicKeys;
    }

    public static void addToAllPublicKeys(String username, String publicKey) {
        allPublicKeys.put(username, publicKey);
    }

    public static Map<String, ServerHandler> getUsers() {
        return users;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public Socket getSocket() {
        return socket;
    }

    public CommunicationManager getCommunicationManager() {
        return communicationManager;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

}
