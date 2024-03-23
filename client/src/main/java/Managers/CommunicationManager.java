package main.java.Managers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.PrintWriter;

public class CommunicationManager {
    private PrintWriter out;
    private ConnectionManager clientHandler;
    private PrintWriter writer;
    private ObjectMapper objectMapper;

    public CommunicationManager(ConnectionManager clientHandler, PrintWriter writer) {
        this.clientHandler = clientHandler;
        this.writer = writer;
        this.objectMapper = new ObjectMapper();
    }

    public CommunicationManager() {}


    public void sendToServer(String header, JsonNode json) {
        synchronized (this) {
            if (writer != null) {
                writer.printf("%s %s\n", header, json.toString());
                writer.flush();
            } else {
                System.err.println("Writer is null");
            }
        }
    }

    public void sendToServer(String header) {
        synchronized (this) {
            if (writer != null) {
                writer.printf("%s \n", header);
                writer.flush();
            } else {
                System.err.println("Writer is null");
            }
        }
    }
    public void sendMessage(String header) {
        out.printf("%s \n", header);
    }

    public void sendMessage(String header, JsonNode json) {
        out.printf("%s %s\n", header, json.toString());
    }

    public void sendMessage(String header, String status) {
        JsonNode json = objectMapper.createObjectNode()
                .put("status", status);
        out.printf("%s %s\n", header, json.toString());
    }

    public void sendMessage(String header, String status, int code) {
        JsonNode json = objectMapper.createObjectNode()
                .put("status", status)
                .put("code", code);
        out.printf("%s %s\n", header, json.toString());
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

}
