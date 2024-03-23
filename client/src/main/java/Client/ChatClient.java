package main.java.Client;

import java.io.IOException;
import java.net.Socket;

public class ChatClient {
    private final String serverAddress;
    private final int serverPort;

    public ChatClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void start() {
        try {
            Socket socket = new Socket(serverAddress, serverPort);
            System.out.println("Connected to the chat server");

            ClientHandler clientHandler = new ClientHandler(socket);
            clientHandler.startHandling();
        } catch (IOException e) {
            System.err.println("Could not connect to the server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient("127.0.0.1", 1337);
        client.start();
    }
}
