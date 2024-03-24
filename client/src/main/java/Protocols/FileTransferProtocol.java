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
    private Path outputPath;

    private final Path downloadedFile = Paths.get("downloadedFile.txt");


    @Override
    public void run() {
        new Thread(() -> {
            setSavePath();
            if (role == TransferRole.SENDER) {
                sendFile(filePath);
            } else if (role == TransferRole.RECEIVER) {
                receiveFile();
            }
        }).start();
    }

    public void sendFile(Path filePath) {
        outputPath = savePath.resolve(filePath.getFileName());
        try (InputStream fileInputStream = Files.newInputStream(filePath); OutputStream fileOutputStream = Files.newOutputStream(downloadedFile)) {

            String fileChecksum = calculateChecksum(filePath.toString());
            setChecksum(fileChecksum);

            long fileSize = Files.size(filePath);
            long totalBytesSent = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;

            while ((len = fileInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
                totalBytesSent += len;
            }
            fileOutputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void receiveFile() {

        try (OutputStream fileOutputStream = Files.newOutputStream(outputPath); InputStream fileInputStream = Files.newInputStream(downloadedFile)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            long totalBytesReceived = 0;

            while ((len = fileInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
                totalBytesReceived += len;
                long progress = (totalBytesReceived * 100L) / fileSize;

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
