package main.java.Protocols;

import main.java.Managers.CommunicationManager;
import main.java.Managers.ConnectionManager;
import main.java.Helpers.GameGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GameProtocol {


    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> gameTask;
    private CommunicationManager communicationManager;
    private ConnectionManager connectionManager;
    private static Map<String, GameProtocol> gameParticipants = new HashMap<>();
    private static Map<String, GameProtocol> initialGameParticipants;
    private static Map<String, Long> guessTimes = new HashMap<>();
    private static GameGenerator gameGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static boolean gameStarted = false;

    private static boolean winnersBroadcasted;


    public void startGame() {
        if (!connectionManager.isLoggedIn()) {
            communicationManager.sendMessage("GAME_RESP", "ERROR", 6001);
            return;
        }
        if (!gameParticipants.isEmpty()) {
            communicationManager.sendMessage("GAME_RESP", "ERROR", 6002);
            return;
        }

        gameParticipants.put(connectionManager.getUsername(), this);
        ConnectionManager.getUsers().values().forEach(client -> {
            if (!client.equals(this)) {
                client.initializeManager.getCommunicationManager().sendMessage("BROADCAST", objectMapper.createObjectNode().put("sender", connectionManager.getUsername()).put("message", " has initiated a guessing game. Type JOINGAME to participate."));
            }
        });
        communicationManager.sendMessage("GAME_RESP", "OK", 6007);
        System.out.println(connectionManager.getUsername() + " started a game.");

        gameTask = executorService.schedule(() -> {
            if (gameParticipants.size() < 2) {
                communicationManager.sendMessage("GAME_RESP", "ERROR", 6010);
                gameParticipants.clear();
            } else {
                gameStarted = true;
                initialGameParticipants = new HashMap<>(gameParticipants);
                ConnectionManager.getUsers().values().forEach(client -> {
                    if (gameParticipants.containsKey(connectionManager.getUsername())) {
                        client.initializeManager.getCommunicationManager().sendMessage("BROADCAST", objectMapper.createObjectNode().put("sender", connectionManager.getUsername()).put("message", "'s game has started. Type 'GUESS <number>' to make a guess."));
                    }
                });
            }
        }, 10, TimeUnit.SECONDS);

        gameGenerator = new GameGenerator();

    }

    public void guessGame(JsonNode json) {
        winnersBroadcasted = false;
        int guess = json.get("guess").asInt();

        if (!connectionManager.isLoggedIn()) {
            communicationManager.sendMessage("GAME_RESP", "ERROR", 6000);
            return;
        }
        if (!gameParticipants.containsKey(connectionManager.getUsername())) {
            communicationManager.sendMessage("GAME_RESP", "ERROR", 6001);
            return;
        }

        if (!gameStarted) {
            communicationManager.sendMessage("GAME_RESP", "ERROR", 6011); // Send error indicating that the game has not started yet
            return;
        }

        if (guess == gameGenerator.getRandom_int()) {
            long guessTime = gameGenerator.calculateGuessTime();
            guessTimes.put(connectionManager.getUsername(), guessTime);
            communicationManager.sendMessage("GUESS_RESP", "OK", 6003);
            gameParticipants.values().forEach(client -> client.communicationManager.sendMessage("GAME_RESP", "OK", 6009));
            gameParticipants.remove(connectionManager.getUsername());

            if (!winnersBroadcasted && gameParticipants.isEmpty()) {
                broadcastWinners();
                winnersBroadcasted = true;
            }
            return;
        }

        if (guess < gameGenerator.getRandom_int()) {
            communicationManager.sendMessage("GUESS_RESP", "OK", 6004);
        } else {
            communicationManager.sendMessage("GUESS_RESP", "OK", 6005);
        }

//        if (gameTask != null) {
//            gameTask.cancel(false); // Cancel the previously scheduled task
//        }

        if (!winnersBroadcasted) {
            gameTask = executorService.schedule(() -> {
                if (!winnersBroadcasted) {
                    broadcastWinners();
                    winnersBroadcasted = true;
                    gameParticipants.clear();
                }
            }, 120, TimeUnit.SECONDS);
        }

    }

    public void joinGame() {
        if (!connectionManager.isLoggedIn()) {
            communicationManager.sendMessage("GAMEJOIN_RESP", "ERROR", 6001);
            return;
        }
        if (!gameGenerator.gameExists()) {
            communicationManager.sendMessage("GAMEJOIN_RESP", "ERROR", 6002);
            return;
        }
        if (gameParticipants.containsKey(connectionManager.getUsername())) {
            communicationManager.sendMessage("GAMEJOIN_RESP", "ERROR", 6003);
            return;
        }
        gameParticipants.put(connectionManager.getUsername(), this);
        communicationManager.sendMessage("GAMEJOIN_RESP", "OK", 6008);
        System.out.println(connectionManager.getUsername() + " joined a game.");

    }

    public void guessResponse(JsonNode jsonNode) {
        if (jsonNode.get("code").asInt() == 6003) {
            System.out.println("You guessed the number! Congratulations!");
        } else if (jsonNode.get("code").asInt() == 6004) {
            System.out.println("Your guess is too low.");
        } else if (jsonNode.get("code").asInt() == 6005) {
            System.out.println("Your guess is too high.");
        } else if (jsonNode.get("code").asInt() == 6009) {
            System.out.println(connectionManager.getUsername() + " guessed the number! Congratulations!");
        }
    }

    public void gameResponse(JsonNode jsonNode) {
        if (jsonNode.get("code").asInt() == 6007) {
            System.out.println("Game started. Waiting 10 seconds for more participants");
        } else if (jsonNode.get("code").asInt() == 6000) {
            System.out.println("You are not logged in");
        } else if (jsonNode.get("code").asInt() == 6001) {
            System.out.println("You are not participating in a guessing game");
        } else if (jsonNode.get("code").asInt() == 6002) {
            System.out.println("Invalid guess");
        } else if (jsonNode.get("code").asInt() == 6010) {
            System.out.println("Not enough participants to start the game");
        } else if (jsonNode.get("code").asInt() == 6011) {
            System.out.println("Game not started yet");
        }
    }

    public void gameJoinResponse(JsonNode jsonNode) {
        if (jsonNode.get("code").asInt() == 6008) {
            System.out.println("Game joined, waiting for game to start...");
        } else if (jsonNode.get("code").asInt() == 6001) {
            System.out.println("You are not logged in");
        } else if (jsonNode.get("code").asInt() == 6002) {
            System.out.println("No game found");
        } else if (jsonNode.get("code").asInt() == 6003) {
            System.out.println("Game already joined");
        }
    }

    public void broadcastWinners() {
        ConnectionManager.getUsers().values().forEach(client -> {
            if (initialGameParticipants.containsKey(client.initializeManager.getConnectionManager().getUsername())) {
                client.initializeManager.getCommunicationManager().sendMessage("BROADCAST", objectMapper.createObjectNode().put("sender", "").put("message", getAllUsernamesAndGuessTimes()));
            }
        });
    }

    public String getAllUsernamesAndGuessTimes() {
        StringBuilder sb = new StringBuilder();
        sb.append("Game results: \n");

        List<Map.Entry<String, Long>> sortedEntries = new ArrayList<>(guessTimes.entrySet());

        // Sorteer lijst op guess time aflopend
        Collections.sort(sortedEntries, Map.Entry.comparingByValue());

        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<String, Long> entry = sortedEntries.get(i);
            sb.append(i + 1).append(". ").append(entry.getKey()).append(" - (").append(entry.getValue()).append("ms)").append("\n");
        }
        return sb.toString();
    }


    public void setCommunicationManager(CommunicationManager communicationManager) {
        this.communicationManager = communicationManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void gameWinners(JsonNode jsonNode) {
        System.out.println(jsonNode.get("message").asText());
    }
}
