package main.java.Protocols;

import main.java.Managers.CommunicationManager;
import main.java.Managers.SecurityManager;
import main.java.Managers.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Pattern;

public class LoginProtocol {
    private ObjectMapper objectMapper = new ObjectMapper();
    private SecurityManager securityManager;
    private CommunicationManager communicationManager;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Z0-9_]{3,14}$", Pattern.CASE_INSENSITIVE);
    private boolean isLoggedIn = false;
    private SessionManager sessionManager;


    public void loginProtocol() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username to log in: ");
        String username = scanner.nextLine();
        String publicKeyEncoded = Base64.getEncoder().encodeToString(securityManager.getRsaKeyPair().getPublic().getEncoded());

        ObjectNode loginNode = objectMapper.createObjectNode();
        loginNode.put("username", username);
        loginNode.put("publicKey", publicKeyEncoded);

        communicationManager.sendToServer("LOGIN_REQ", loginNode);
    }

    public void loginResponse(JsonNode jsonNode) throws IOException {
        if (jsonNode.get("code").asInt() == 5000) {
            System.out.println("You are already logged in");
        } else if (jsonNode.get("code").asInt() == 5001) {
            System.out.println("Username has an invalid format or length");
        } else if (jsonNode.get("code").asInt() == 5002) {
            System.out.println("Username already in use");
        } else if (jsonNode.get("code").asInt() == 5003) {
            System.out.println("Username is empty");
        } else {
            System.out.println("Could not login.");
        }
        loginProtocol();
    }

    public void loginProcessResponse(JsonNode json) {

        JsonNode usernameNode = json.get("username");

        String username = usernameNode.asText();

        if (isLoggedIn) {
            communicationManager.sendMessage("LOGIN_RESP", "ERROR", 5000);
            return;
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            communicationManager.sendMessage("LOGIN_RESP", "ERROR", 5001);
        }
    }

    public void setCommunicationManager(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
}
