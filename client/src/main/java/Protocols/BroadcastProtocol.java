package main.java.Protocols;

import main.java.Managers.InitializeManager;
import main.java.Server.ServerHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.regex.Pattern;

public class BroadcastProtocol {

    public void broadcastResponse(JsonNode jsonNode) {
        if (!jsonNode.has("status")) {
            System.out.println(jsonNode.get("sender").asText() + " whispers " + jsonNode.get("message").asText());
        } else if (jsonNode.get("status").asText().equals("OK")) {
            System.out.println("Broadcast successfully sent!");
        } else if (jsonNode.get("code").asInt() == 6001) {
            System.out.println("User is not logged in.");
        } else if (jsonNode.get("code").asInt() == 6002) {
            System.out.println("Message format invalid.");
        } else {
            System.out.println("Broadcast failed.");
        }
    }

    public void processBroadcast(JsonNode json, boolean isLoggedIn, Map<String, ServerHandler> users, String username, InitializeManager initializeManager, ObjectMapper objectMapper) {
        if (!isLoggedIn) {
            initializeManager.getCommunicationManager().sendMessage("BROADCAST_RESP", "ERROR", 6001);
        }

        Pattern allowedCharacters = Pattern.compile("^[a-zA-ZÀ-ÿ0-9 ]+$");

        if (!allowedCharacters.matcher(json.get("message").asText()).matches()) {
            initializeManager.getCommunicationManager().sendMessage("BROADCAST_RESP", "ERROR", 6002);
        } else {
            users.values().forEach(client -> {
                if (!client.getUsername().equals(username)) {
                    client.initializeManager.getCommunicationManager().sendMessage("BROADCAST", objectMapper.createObjectNode().put("sender", username + " says: ").put("message", json.get("message").asText()));
                }
            });
            initializeManager.getCommunicationManager().sendMessage("BROADCAST_RESP", "OK");
            System.out.println(username + " broadcast a message.");
        }
    }
}
