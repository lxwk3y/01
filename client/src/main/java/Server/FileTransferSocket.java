package main.java.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FileTransferSocket implements Runnable {
    private static final int FILE_TRANSFER_PORT = 1338;
    private ServerSocket serverSocket;

    public FileTransferSocket() {
        try {
            serverSocket = new ServerSocket(FILE_TRANSFER_PORT);
            System.out.println("File Transfer main.java.Server started on port " + FILE_TRANSFER_PORT);
        } catch (IOException e) {
            System.err.println("File Transfer main.java.Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket fileClientSocket = serverSocket.accept();
                // Handle file transfer logic here (e.g., create a new thread to handle the transfer).
            } catch (IOException e) {
                System.err.println("File Transfer main.java.Server exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
