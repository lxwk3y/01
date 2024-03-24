package main.java.Protocols;


import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileTransferProtocol implements Runnable {

    private final int BUFFER_SIZE = 65536;
    private final String serverAddress = "127.0.0.1";
    private final int serverPort = 1338;
    private TransferRole role = null;
    private Path filePath;
    private String checksum;
    private long fileSize;
    private Path savePath;

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setRole(TransferRole role) {
        this.role = role;
    }

    @Override
    public void run() {
        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort)) {
                socket.setReceiveBufferSize(BUFFER_SIZE);
                socket.setSendBufferSize(BUFFER_SIZE);
                if (role == TransferRole.SENDER) {
                    sendFile(filePath, socket);
                } else if (role == TransferRole.RECEIVER) {
                    receiveFile(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendFile(Path filePath, Socket socket) {
        try (InputStream fileInputStream = Files.newInputStream(filePath); OutputStream socketOutputStream = socket.getOutputStream()) {

            long fileSize = Files.size(filePath);
            long totalBytesSent = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;

            while ((len = fileInputStream.read(buffer)) != -1) {
                socketOutputStream.write(buffer, 0, len);
                totalBytesSent += len;
                long progress = (totalBytesSent * 100L) / fileSize;
                System.out.println("Progress: " + progress + "%");
                System.out.println("Total bytes sent: " + totalBytesSent);
                System.out.println("Bytes sent in this iteration: " + len); // Add this debug log
            }

            socketOutputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void receiveFile(Socket socket) {

        setSavePath();

        Path outputPath = savePath.resolve(filePath.getFileName());

        try (OutputStream fileOutputStream = Files.newOutputStream(outputPath); InputStream socketInputStream = socket.getInputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            long totalBytesReceived = 0;

            while ((len = socketInputStream.read(buffer)) != -1) {
                System.out.println("while loop");
                fileOutputStream.write(buffer, 0, len);
                totalBytesReceived += len;
                long progress = (totalBytesReceived * 100L) / fileSize;
                System.out.println("Progress: " + progress + "%");

                System.out.println("Bytes received: " + len); // Debug log
            }

            System.out.println("File transfer completed."); // Debug log

            System.out.println(checkChecksum(calculateChecksum(outputPath.toString())) ? "Success" : "Failure");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String calculateChecksum(String filePath) {
        Path path = Paths.get(filePath);

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream fis = Files.newInputStream(path);
                 DigestInputStream dis = new DigestInputStream(fis, md)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (dis.read(buffer) != -1) ;
                byte[] digest = md.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte b : digest) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSavePath() {
        savePath = Paths.get("ReceivedFiles/");

        try {
            if (!Files.exists(savePath)) {
                Files.createDirectories(savePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkChecksum(String receivedChecksum) {
        boolean match = checksum.equals(receivedChecksum);
        System.out.println(match ? "Checksums match, file transfer validity confirmed." : "Checksums do not match, file transfer validity should be questioned.");
        return match;
    }

    //use enum for clearer code
    public enum TransferRole {
        SENDER,
        RECEIVER
    }
}
