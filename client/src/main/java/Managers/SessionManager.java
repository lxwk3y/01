package main.java.Managers;

import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

public class SessionManager {

    private Map<String, String> publicKeys;
    private boolean loggedIn = false;

    public SessionManager() {
        publicKeys = new HashMap<>();
    }

    public Map<String, String> getPublicKeys() {
        return publicKeys;
    }

    public void addPublicKey(String username, String publicKey) {
        this.publicKeys.put(username, publicKey);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }



}
