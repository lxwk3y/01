package main.java.Managers;

import main.java.Protocols.*;

import java.io.IOException;

public class ProtocolManager {
    private BroadcastProtocol broadcastProtocol;
    private DMProtocol dmProtocol;
    private FileTransferProtocol fileTransferProtocol;
    private HeartbeatProtocol heartbeatProtocol;
    private ListClientsProtocol listClientsProtocol;
    private LoginProtocol loginProtocol;
    private GameProtocol gameProtocol;

    public ProtocolManager() throws IOException {
        this.broadcastProtocol = new BroadcastProtocol();
        this.dmProtocol = new DMProtocol();
        this.fileTransferProtocol = new FileTransferProtocol();
        this.heartbeatProtocol = new HeartbeatProtocol();
        this.listClientsProtocol = new ListClientsProtocol();
        this.loginProtocol = new LoginProtocol();
        this.gameProtocol = new GameProtocol();
    }

    public GameProtocol getGameProtocol() {
        return gameProtocol;
    }

    public BroadcastProtocol getBroadcastProtocol() {
        return broadcastProtocol;
    }

    public DMProtocol getDmProtocol() {
        return dmProtocol;
    }

    public FileTransferProtocol getFileTransferProtocol() {
        return fileTransferProtocol;
    }

    public HeartbeatProtocol getHeartbeatProtocol() {
        return heartbeatProtocol;
    }

    public ListClientsProtocol getListClientsProtocol() {
        return listClientsProtocol;
    }

    public LoginProtocol getLoginProtocol() {
        return loginProtocol;
    }
}
