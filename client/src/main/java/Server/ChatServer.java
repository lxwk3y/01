package main.java.Server;

import main.java.Client.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    private static final int CHAT_PORT = 1337;
    private static final int FILE_TRANSFER_PORT = 1338;
    private final Set<ServerHandler> chatClients = ConcurrentHashMap.newKeySet();
    private final Set<ClientHandler> fileTransferClients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        new Thread(server::startChatServer).start();
        new Thread(server::startFileTransferServer).start();
    }

    public void startChatServer() {
        try (ServerSocket serverSocket = new ServerSocket(CHAT_PORT)) {
            System.out.println("Chat Server started on port " + CHAT_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ServerHandler client = new ServerHandler(clientSocket, this);
                chatClients.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            System.err.println("Chat Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startFileTransferServer() {
        try (ServerSocket serverSocket = new ServerSocket(FILE_TRANSFER_PORT)) {
            System.out.println("File Transfer Server started on port " + FILE_TRANSFER_PORT);
            while (true) {
                Socket soc = serverSocket.accept();
            }
        } catch (IOException e) {
            System.err.println("File Transfer Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void removeClient(ServerHandler client) {
        chatClients.remove(client);
        System.out.println("Aantal verbonden clients: " + chatClients.size());
    }
}
