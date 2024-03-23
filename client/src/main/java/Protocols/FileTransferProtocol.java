package main.java.Protocols;

import java.io.*;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileTransferProtocol implements Runnable {

    private final int BUFFER_SIZE = 65536;
    private final String serverAddress = "127.0.0.1";
    private final int serverPort = 1338;
    //Change to enum to make the role more readable. a "String" could represent *any* text,
    // whereas an enum makes the options clear to future developers
    private TransferRole role = null;
    private String filePath;
    private String checksum;
    private long fileSize;

    public void setFilePath(String filePath) {
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
        //start thread at top-level since it's needed in any case
        new Thread(() -> {
            //change to try-with-resources to make sure the socket is always closed when it's no longer in use
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

    public void sendFile(String filePath, Socket socket) {
        try (FileInputStream fileInputStream = new FileInputStream(filePath); OutputStream socketOutputStream = socket.getOutputStream()) {
            File file = new File(filePath);
            long fileSize = file.length();
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
        //change to try-with-resources to avoid needing a try-catch inside a try-catch
        try (FileOutputStream fileOutputStream = new FileOutputStream("xddee.txt")) {
            System.out.println("FileOutputStream created."); // Debug log

            InputStream socketInputStream = socket.getInputStream();
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

            System.out.println(checkChecksum(calculateChecksum(filePath)) ? "Success" : "Failure");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String calculateChecksum(String filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream fis = new FileInputStream(filePath); DigestInputStream dis = new DigestInputStream(fis, md)) {
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
