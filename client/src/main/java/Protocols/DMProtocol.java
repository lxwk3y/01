package main.java.Protocols;

import main.java.Managers.CommunicationManager;
import main.java.Server.ServerHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class DMProtocol {
    private CommunicationManager communicationManager;
    private ObjectMapper objectMapper = new ObjectMapper();

    public void processDM(JsonNode json, boolean isLoggedIn,  Map<String, ServerHandler> users, String username) {
        if (!isLoggedIn) {
            communicationManager.sendMessage("DM_RESP", "ERROR", 4001);
            return;
        }

        String recipient = json.get("recipient").asText();
        String encryptedData = json.get("message").asText();

        if (!users.containsKey(recipient)) {
            communicationManager.sendMessage("DM_RESP", "ERROR", 4000);
            return;
        }

        if (json.get("message").asText().length() > 1000) {
            communicationManager.sendMessage("DM_RESP", "ERROR", 4002);
        }

        JsonNode jsonNode = objectMapper.createObjectNode()
                .put("sender", username)
                .put("message", encryptedData);

        users.get(recipient).getCommunicationManager().sendMessage("DM", jsonNode);
        communicationManager.sendMessage("DM_RESP", "OK");
        System.out.println(username + " has sent a DM to " + recipient + ".");
    }

    public void DMResponse(JsonNode jsonNode) {
        if (jsonNode.get("status").asText().equals("OK")) {
            System.out.println("Message successfully sent!");
        } else if (jsonNode.get("code").asInt() == 4001) {
            System.out.println("User not logged in");
        } else if (jsonNode.get("code").asInt() == 4000) {
            System.out.println("Recipient not found.");
        } else if (jsonNode.get("code").asInt() == 4002) {
            System.out.println("Message too long");
        }
    }
    public void setCommunicationManager(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
    }
}
