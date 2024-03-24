package main.java.Client;

import main.java.Helpers.UniqueIdGenerator;
import main.java.Managers.ConnectionManager;
import main.java.Managers.InitializeManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.java.Protocols.FileTransferProtocol;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

public class ServerCommunication {
    private ClientHandler clientHandler;
    private InitializeManager initializeManager;
    private ObjectMapper objectMapper;
    private HashMap<String, Path> fileTransferMap;
    private UniqueIdGenerator uniqueIdGenerator = new UniqueIdGenerator();
    private volatile boolean running;

    public ServerCommunication(ClientHandler clientHandler, InitializeManager initializeManager) {
        this.clientHandler = clientHandler;
        this.initializeManager = initializeManager;
        objectMapper = new ObjectMapper();
        fileTransferMap = new HashMap<>();
    }

    public void readFromServer() {
        try {
            String messageFromServer;
            while ((messageFromServer = initializeManager.getConnectionManager().getReader().readLine()) != null) {

                String[] parts = messageFromServer.split(" ", 2);

                if (parts.length == 2) {
                    String header = parts[0];
                    String jsonPart = parts[1];

                    // Parse de JSON
                    JsonNode jsonNode = objectMapper.readTree(jsonPart);
                    switch (header) {
                        case "LOGIN_RESP":
                            if (jsonNode.get("status").asText().equals("OK")) {
                                initializeManager.getSessionManager().setLoggedIn(true);
                                synchronized (this) {
                                    notify();
                                }
                                System.out.println("Successfully logged in! Type 'help' for a list of available commands.");
                                break;
                            } else {
                                initializeManager.getProtocolManager().getLoginProtocol().loginResponse(jsonNode);
                            }
                            break;
                        case "PUBLIC_KEY":
                            String keyUser = jsonNode.get("username").asText();
                            String keyData = jsonNode.get("publicKey").asText();
                            initializeManager.getSessionManager().addPublicKey(keyUser, keyData);
                            break;

                        case "BROADCAST_RESP":
                            initializeManager.getProtocolManager().getBroadcastProtocol().broadcastResponse(jsonNode);
                            break;
                        case "BROADCAST":
                            System.out.println(jsonNode.get("sender").asText() + jsonNode.get("message").asText());
                            break;
                        case "DM":
                            String decryptedMessage = initializeManager.getSecurityManager().decryptMessageWithAES(jsonNode.get("message").asText());
                            System.out.println(jsonNode.get("sender").asText() + " whispers: " + decryptedMessage);
                            break;
                        case "DM_RESP":
                            initializeManager.getProtocolManager().getDmProtocol().DMResponse(jsonNode);
                            break;
                        case "LIST_CLIENTS":
                            System.out.println("Connected clients: " + jsonNode.get("users").asText());
                            break;
                        case "LIST_CLIENTS_RESP":
                            initializeManager.getProtocolManager().getListClientsProtocol().listClientsResponse(jsonNode);
                            break;
                        case "GAME_RESP":
                            initializeManager.getProtocolManager().getGameProtocol().gameResponse(jsonNode);
                            break;
                        case "GAMEJOIN_RESP":
                            initializeManager.getProtocolManager().getGameProtocol().gameJoinResponse(jsonNode);
                            break;
                        case "GUESS_RESP":
                            initializeManager.getProtocolManager().getGameProtocol().guessResponse(jsonNode);
                            break;
                        case "gameWinners":
                            initializeManager.getProtocolManager().getGameProtocol().gameWinners(jsonNode);
                            break;
                        case "BYE_RESP":
                            if (jsonNode.get("status").asText().equals("OK")) {
                                System.out.println("You are now logged out.");
                            } else {
                                System.out.println("Logout failed.");
                            }
                            break;
                        case "HELP_RESPONSE":
                            clientHandler.processHelp();
                            break;
                        case "PING":
                            //System.out.println("Ping received");
                            initializeManager.getCommunicationManager().sendToServer("PONG");
                            break;
                        case "DSCN":
                            initializeManager.getProtocolManager().getHeartbeatProtocol().handleDisconnection(jsonNode);
                            break;
                        case "PONG_ERROR":
                            initializeManager.getProtocolManager().getHeartbeatProtocol().handlePongError(jsonNode);
                            break;
                        case "UNKNOWN_COMMAND":
                            if (jsonNode.get("code").asInt() == 404) {
                                System.out.println("Command not found");
                            }
                            break;
                        case "SENDFILE_RESP":
                            System.out.println(jsonNode.get("You have been sent the following file: " + jsonNode.get("filename").asText() + " (" + jsonNode.get("filesize").asLong() + " bytes)");
                            System.out.println("Do you want to accept this file? (FILETRANSFER_ACCEPT/FILETRANSFER_DENY <filename>)");

                            Path filePath = Paths.get(jsonNode.get("filePath").asText());
                            fileTransferMap.put(jsonNode.get("filename").asText(), filePath);

                            initializeManager.getProtocolManager().getFileTransferProtocol().setFilePath(filePath);
                            initializeManager.getProtocolManager().getFileTransferProtocol().setRole(FileTransferProtocol.TransferRole.SENDER);
                            initializeManager.getProtocolManager().getFileTransferProtocol().setFileSize(jsonNode.get("filesize").asLong());

                            initializeManager.getProtocolManager().getFileTransferProtocol().run();
                            break;
                        case "FILETRANSFER_ACCEPT":
                            initializeManager.getProtocolManager().getFileTransferProtocol().setRole(FileTransferProtocol.TransferRole.RECEIVER);
                            initializeManager.getProtocolManager().getFileTransferProtocol().run();
                            break;
                        default:
                            System.err.println("Invalid header received: " + header);
                            break;
                    }
                } else {
                    System.err.println("Invalid message format received: " + messageFromServer);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from server: " + e.getMessage());
        }
    }

    public void writeToServer() {
        synchronized (this) {
            while (!initializeManager.getSessionManager().isLoggedIn()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
        running = true;
        Scanner scanner = new Scanner(System.in);
        Pattern allowedCharacters = Pattern.compile("^[a-zA-ZÀ-ÿ0-9 .,?!;:'\"(){}\\[\\]<>@#$%^&*_+=|\\\\/~`-]+$");


        while (running) {
            try {
                int limit;
                String userInput = scanner.nextLine();
                if (userInput.toUpperCase().startsWith("DM") || userInput.toUpperCase().startsWith("SENDFILE")) {
                    limit = 3;
                } else {
                    limit = 2;
                }
                String[] parts = userInput.split(" ", limit);
                parts[0] = parts[0].toUpperCase(); // header
                switch (parts[0]) {
                    case "LOGIN":
                        System.out.println("You are already logged in.");
                        break;
                    case "BROADCAST":
                        synchronized (this) {
                            initializeManager.getCommunicationManager().sendToServer("BROADCAST_REQ", objectMapper.createObjectNode().put("message", parts[1]));
                        }
                        break;
                    case "DM":
                        if (parts.length != 3 || !allowedCharacters.matcher(parts[2]).matches() || parts[2].length() >= 1000) {
                            System.out.println("Invalid message format or characters used.");
                            continue;
                        }
                        if (parts.length > 1) {
                            synchronized (this) {
                                String recipient = parts[1];
                                String message = parts[2];
                                String recipientPublicKey = initializeManager.getSessionManager().getPublicKeys().get(recipient);

                                if (recipientPublicKey == null) {
                                    System.out.println("Recipient's public key not found");
                                    break;
                                }

                                String encryptedData = initializeManager.getSecurityManager().encryptMessageWithAES(message, recipientPublicKey);
                                initializeManager.getCommunicationManager().sendToServer("DM_REQ", objectMapper.createObjectNode()
                                        .put("recipient", recipient)
                                        .put("message", encryptedData));
                            }
                            break;
                        } else {
                            System.out.println("Invalid message format");
                        }
                        break;
                    case "CLIENTS":
                        synchronized (this) {
                            initializeManager.getCommunicationManager().sendToServer("LIST_CLIENTS_REQ");
                        }
                        break;
                    case "STARTGAME":
                        synchronized (this) {
                            initializeManager.getCommunicationManager().sendToServer("START_GAME_REQ");
                        }
                        break;
                    case "JOINGAME":
                        synchronized (this) {
                            initializeManager.getCommunicationManager().sendToServer("JOIN_GAME_REQ");
                        }
                        break;
                    case "GUESS":
                        synchronized (this) {
                            try {
                                int intValue = Integer.parseInt(parts[1]);
                                initializeManager.getCommunicationManager().sendToServer("GUESS_REQ", objectMapper.createObjectNode().put("guess", intValue));
                            } catch (NumberFormatException e) {
                                System.out.println("Value is not a number");
                            }
                        }
                        break;
                    case "HELP":
                        clientHandler.processHelp();
                        break;
                    case "BYE":
                        synchronized (this) {
                            initializeManager.getCommunicationManager().sendToServer("BYE_REQ", objectMapper.createObjectNode());
                        }
                        break;
                    case "SENDFILE":
                        Path path = Paths.get(parts[2]);
                        String filename = path.getFileName().toString();
                        fileTransferMap.put(filename, path);
                        initializeManager.getCommunicationManager().sendToServer("SENDFILE_REQ", objectMapper.createObjectNode()
                                .put("receiver", parts[1])
                                .put("filename", filename)
                                .put("filesize", Files.size(path))
                                .put("filePath", path.toAbsolutePath().toString())
                        );
                        System.out.println("File has been sent to " + parts[1]);
                        break;
                    case "FILETRANSFER_ACCEPT":
                        Path acceptedPath = fileTransferMap.get(parts[1]);
                        System.out.println("size: " + fileTransferMap.size());

                        if (acceptedPath != null) {
                            initializeManager.getCommunicationManager().sendToServer("FILETRANSFER_ACCEPT", objectMapper.createObjectNode()
                                    .put("filename", parts[1])
                                    .put("filesize", Files.size(acceptedPath))
                                    .put("filePath", acceptedPath.toAbsolutePath().toString())
                            );
                        } else {
                            System.out.println("File with name '" + parts[1] + "' not found.");
                        }
                        break;
                    default:
                        System.out.println("Invalid command");
                        break;
                }
                if (userInput.equalsIgnoreCase("bye")) {
                    synchronized (this) {
                        initializeManager.getCommunicationManager().sendToServer("BYE_REQ", objectMapper.createObjectNode());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        scanner.close();
        clientHandler.stop();
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
