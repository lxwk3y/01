package Managers;

import java.io.IOException;
import java.io.OutputStream;

public class InitializeManager {
    private SecurityManager securityManager;
    private ProtocolManager protocolManager;
    private ConnectionManager connectionManager;
    private SessionManager sessionManager;
    private CommunicationManager communicationManager;


    public InitializeManager() throws IOException {
        this.securityManager = new SecurityManager();
        this.protocolManager = new ProtocolManager();
        this.connectionManager = new ConnectionManager();
    }


    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void setCommunicationManager(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public CommunicationManager getCommunicationManager() {
        return communicationManager;
    }
}
