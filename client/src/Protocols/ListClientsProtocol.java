package Protocols;

import Managers.InitializeManager;
import Server.ServerHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ListClientsProtocol {

    private final ObjectMapper objectMapper = new ObjectMapper();


    public void processListClients (boolean isLoggedIn, String username, InitializeManager initializeManager, Map<String, ServerHandler> users) {
        if (!isLoggedIn) {
            initializeManager.getCommunicationManager().sendMessage("LIST_CLIENTS_RESP", "ERROR", 3001);
            return;
        }
        if (users.isEmpty()) {
            initializeManager.getCommunicationManager().sendMessage("LIST_CLIENTS_RESP", "ERROR", 3002);
            return;
        }

        StringBuilder clientsList = new StringBuilder();
        users.keySet().forEach(u -> clientsList.append("\n").append(u));
        JsonNode jsonNode = objectMapper.createObjectNode()
                .put("users", clientsList.toString());
        initializeManager.getCommunicationManager().sendMessage("LIST_CLIENTS", jsonNode);
        initializeManager.getCommunicationManager().sendMessage("LIST_CLIENTS_RESP", "OK");
        System.out.println(username + " requested a list of connected clients.");
    }

    public void listClientsResponse(JsonNode jsonNode) {
        if (jsonNode.get("status").asText().equals("OK")) {
            System.out.println("Clients successfully listed!");
        } else if (jsonNode.get("code").asInt() == 3001) {
            System.out.println("User is not logged in.");
        } else if (jsonNode.get("code").asInt() == 3002) {
            System.out.println("No clients found.");
        } else {
            System.out.println("Failed to list clients.");
        }
    }

}
