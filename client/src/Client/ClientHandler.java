package Client;

import Managers.*;
import java.io.*;
import java.net.Socket;

public class ClientHandler {
    private InitializeManager initializeManager = new InitializeManager();
    private ServerCommunication serverCommunication;

    public ClientHandler(Socket socket) throws IOException {
        initializeManager.getConnectionManager().setSocket(socket);
        initializeManager.getConnectionManager().setupStreams();
        initializeManager.getSecurityManager().generateRSAKeyPair();
        initializeManager.setSessionManager(new SessionManager());
        initializeManager.setCommunicationManager(new CommunicationManager(initializeManager.getConnectionManager(), initializeManager.getConnectionManager().getWriter()));
        initializeManager.getProtocolManager().getLoginProtocol().setSecurityManager(initializeManager.getSecurityManager());
        initializeManager.getProtocolManager().getLoginProtocol().setCommunicationManager(initializeManager.getCommunicationManager());
        initializeManager.getProtocolManager().getLoginProtocol().setSessionManager(initializeManager.getSessionManager());

        serverCommunication = new ServerCommunication(this, initializeManager);
    }

    public void startHandling() throws IOException {
        initializeManager.getProtocolManager().getLoginProtocol().loginProtocol();

        Thread readThread = new Thread(serverCommunication::readFromServer);
        readThread.start();

        Thread writeThread = new Thread(serverCommunication::writeToServer);
        writeThread.start();
    }
    public void processHelp() {
        System.out.println(("Commands: \n" +
                "LOGIN <username> - Login with the given username.\n" +
                "BROADCAST <message> - Send a message to all connected clients.\n" +
                "DM <username> <message> - Send a message to a specific client.\n" +
                "CLIENTS - List all connected clients.\n" +
                "STARTGAME - Start a guessing game. \n" +
                "JOINGAME - Join a guessing game. \n" +
                "GUESS <number> - Make a guess. \n" +
                "SENDFILE <username> <file path> - Send a file to a specific user. \n" +
                "FILETRANSFER_ACCEPT/DENY <transferId> - Accept or deny a file transfer request. \n" +
                "BYE - Disconnect from the server."));
    }

    public void stop() {
        serverCommunication.setRunning(false);
        try {
            if (initializeManager.getConnectionManager().getSocket() != null) initializeManager.getConnectionManager().getSocket().close();
            if (initializeManager.getConnectionManager().getReader()  != null) initializeManager.getConnectionManager().getReader().close();
            if (initializeManager.getConnectionManager().getWriter()  != null) initializeManager.getConnectionManager().getWriter().close();
            System.out.println("Disconnected from the chat server");
        } catch (IOException e) {
            System.err.println("Error while closing the connection: " + e.getMessage());
        }
    }
}