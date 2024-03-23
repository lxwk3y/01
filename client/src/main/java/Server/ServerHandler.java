package main.java.Server;

import main.java.Helpers.CleanupCallback;
import main.java.Managers.CommunicationManager;
import main.java.Managers.ConnectionManager;
import main.java.Managers.InitializeManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;

import static main.java.Managers.ConnectionManager.getAllPublicKeys;


public class ServerHandler implements Runnable, CleanupCallback {

    public InitializeManager initializeManager = new InitializeManager();
    private ObjectMapper objectMapper = new ObjectMapper();
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private BufferedReader in;


    public ServerHandler(Socket socket, ChatServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            initializeManager.getConnectionManager().setSocket(socket);
            initializeManager.getConnectionManager().setupStreams();
            initializeManager.setCommunicationManager(new CommunicationManager(initializeManager.getConnectionManager(), out));
            initializeManager.getCommunicationManager().setOut(out);
            initializeManager.getProtocolManager().getDmProtocol().setCommunicationManager(initializeManager.getCommunicationManager());
            initializeManager.getProtocolManager().getGameProtocol().setCommunicationManager(initializeManager.getCommunicationManager());
            initializeManager.getProtocolManager().getGameProtocol().setConnectionManager(initializeManager.getConnectionManager());
            initializeManager.getProtocolManager().getHeartbeatProtocol().setCommunicationManager(initializeManager.getCommunicationManager());
            initializeManager.getProtocolManager().getHeartbeatProtocol().setCleanupCallback(this);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error setting up client handler: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                processMessage(inputLine);
            }
        } catch (JsonProcessingException k) {
            System.out.println("Error processing json: " + k.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading from client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }


    private void processMessage(String json) {
        try {
            String[] parts = json.split(" ", 2);
            String header = parts[0];
            String jsonPart = parts[1];
            JsonNode jsonNode = objectMapper.readTree(jsonPart);
            switch (header) {
                case "LOGIN_REQ":
                    processLogin(jsonNode);
                    initializeManager.getConnectionManager().setUsername(jsonNode.get("username").asText());
                    break;
                case "BROADCAST_REQ":
                    initializeManager.getProtocolManager().getBroadcastProtocol().processBroadcast(jsonNode, initializeManager.getConnectionManager().isLoggedIn(), ConnectionManager.getUsers(), initializeManager.getConnectionManager().getUsername(), initializeManager, objectMapper);
                    break;
                case "BYE_REQ":
                    processBye();
                    break;
                case "PONG":
                    System.out.println("Pong received from " + initializeManager.getConnectionManager().getUsername());
                    initializeManager.getProtocolManager().getHeartbeatProtocol().processPong();
                    break;
                case "LIST_CLIENTS_REQ":
                    initializeManager.getProtocolManager().getListClientsProtocol().processListClients(initializeManager.getConnectionManager().isLoggedIn(), initializeManager.getConnectionManager().getUsername(), initializeManager, ConnectionManager.getUsers());
                    break;
                case "DM_REQ":
                    initializeManager.getProtocolManager().getDmProtocol().processDM(jsonNode, initializeManager.getConnectionManager().isLoggedIn(), ConnectionManager.getUsers(), initializeManager.getConnectionManager().getUsername());
                    break;
                case "START_GAME_REQ":
                    initializeManager.getProtocolManager().getGameProtocol().startGame();
                    break;
                case "JOIN_GAME_REQ":
                    initializeManager.getProtocolManager().getGameProtocol().joinGame();
                    break;
                case "GUESS_REQ":
                    initializeManager.getProtocolManager().getGameProtocol().guessGame(jsonNode);
                    break;
                case "gameWinners":
                    initializeManager.getProtocolManager().getGameProtocol().gameWinners(jsonNode);
                    break;
                case "SENDFILE_REQ":
                    initializeManager.getCommunicationManager().sendMessage("SENDFILE_REQ", jsonNode);
                    break;
                case "FILETRANSFER_ACCEPT":
                    initializeManager.getCommunicationManager().sendMessage("FILETRANSFER_ACCEPT", jsonNode);
                    break;
                default:
                    initializeManager.getCommunicationManager().sendMessage("UNKNOWN_COMMAND", "ERROR", 404);
                    break;
            }
        } catch (IOException e) {
            System.err.println("Error reading from client: " + e.getMessage());
        }
    }

    private void processLogin(JsonNode json) {
        initializeManager.getProtocolManager().getLoginProtocol().loginProcessResponse(json);

        String username = json.get("username").asText();
        String publicKey = json.get("publicKey").asText();

        synchronized (ConnectionManager.getUsers()) {
            if (ConnectionManager.getUsers().containsKey(username)) {
                initializeManager.getCommunicationManager().sendMessage("LOGIN_RESP ", "ERROR", 5002);
                System.out.println(username + " is already in use (ERROR).");
                return;
            }

            ConnectionManager.getUsers().put(username, this);
            initializeManager.getConnectionManager().setLoggedIn(true);
            ConnectionManager.addToAllPublicKeys(username, publicKey); // Store public key
            initializeManager.getConnectionManager().sendAllPublicKeys(this); // Stuur alle public keys naar nieuw ingelogde clients
            initializeManager.getConnectionManager().broadcastPublicKey(username, publicKey);
            initializeManager.getProtocolManager().getHeartbeatProtocol().schedulePingMessages();
            initializeManager.getCommunicationManager().sendMessage("LOGIN_RESP", "OK");
            System.out.println(username + " logged in.");
        }
    }

    private void processBye() {
        try {
            initializeManager.getCommunicationManager().sendMessage("BYE_RESP", "OK");
            System.out.println(initializeManager.getConnectionManager().getUsername() + " logged out.");
        } finally {
            cleanup();
        }
    }

    @Override
    public void performCleanup() {
        cleanup();
    }
    public void cleanup() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Socket successfully closed.");
            }
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        } finally {
            server.removeClient(this);
            ConnectionManager.getUsers().remove(initializeManager.getConnectionManager().getUsername());
            synchronized (getAllPublicKeys()) {
                getAllPublicKeys().remove(initializeManager.getConnectionManager().getUsername());
            }
        }
    }

    public CommunicationManager getCommunicationManager() {
        return initializeManager.getCommunicationManager();
    }

    public Object getUsername() {
        return initializeManager.getConnectionManager().getUsername();
    }
}
