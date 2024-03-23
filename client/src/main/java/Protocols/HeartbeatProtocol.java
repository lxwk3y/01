package main.java.Protocols;

import main.java.Helpers.CleanupCallback;
import main.java.Managers.CommunicationManager;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static jdk.jfr.internal.consumer.EventLog.stop;

public class HeartbeatProtocol {

    private ScheduledFuture<?> pingFuture;
    private ScheduledFuture<?> disconnectionFuture;
    private ScheduledExecutorService pingExecutor = Executors.newSingleThreadScheduledExecutor();
    private CommunicationManager communicationManager;
    private CleanupCallback cleanupCallback;

    public void setCleanupCallback(CleanupCallback cleanupCallback) {
        this.cleanupCallback = cleanupCallback;
    }

    public void handleDisconnection(JsonNode jsonNode) {
        System.out.println("Disconnected by server: " + jsonNode.get("code").asInt());
        stop();
    }

    public void handlePongError(JsonNode jsonNode) {
        System.out.println("Pong Error: " + jsonNode.get("code").asText());
    }

    public void schedulePingMessages() {
        pingFuture = pingExecutor.scheduleAtFixedRate(() -> {
            communicationManager.sendMessage("PING");
            // Cancel dc task
            if (disconnectionFuture != null && !disconnectionFuture.isDone()) {
                disconnectionFuture.cancel(false);
            }
            // dc na x seconden
            disconnectionFuture = pingExecutor.schedule(this::disconnectInactiveClient, 13, TimeUnit.SECONDS);
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void disconnectInactiveClient() {
        communicationManager.sendMessage("DSCN", "ERROR", 7000);
        if (cleanupCallback != null) {
            cleanupCallback.performCleanup();
        }
    }

    public void processPong() {
        if (pingFuture != null) {
            if (disconnectionFuture != null && !disconnectionFuture.isDone()) {
                disconnectionFuture.cancel(false);
            }
        } else {
            communicationManager.sendMessage("PONG_ERROR", "ERROR", 8000);
        }
    }

    public void setCommunicationManager(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
    }
}
