package net.defade.towerbow.game;

import net.defade.minestom.amethyst.AmethystSource;
import net.defade.minestom.event.server.ServerMarkedForStopEvent;
import net.defade.towerbow.map.LobbyAmethystSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.timer.TaskSchedule;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public class GameManager {
    public static final int MIN_PLAYERS = 4;
    public static final int MAX_PLAYERS = 12;
    private static final int MAX_GAME_INSTANCES = 20;

    private static final AmethystSource LOBBY_SOURCE = new LobbyAmethystSource();

    private final Set<GameInstance> gameInstances = new CopyOnWriteArraySet<>();

    public GameManager() {
        MinecraftServer.getSchedulerManager().scheduleTask(this::checkGameInstances, TaskSchedule.immediate(), TaskSchedule.seconds(2));

        registerPlayerMiniGameMoveRequest();
        isolatePlayers();
        disconnectPlayersWhenMarkedForStop();
    }

    public void checkGameInstances() {
        int gameInstancesAcceptingPlayers = (int) gameInstances.stream()
                .filter(GameInstance::acceptPlayers)
                .count();

        while (gameInstancesAcceptingPlayers < 2 && gameInstances.size() < MAX_GAME_INSTANCES) {
            createGameInstance();
            gameInstancesAcceptingPlayers++;
        }
    }

    public void createGameInstance() {
        GameInstance gameInstance = new GameInstance(this, LOBBY_SOURCE);
        MinecraftServer.getInstanceManager().registerInstance(gameInstance);
        MinecraftServer.getServerApi().registerMiniGameInstance(gameInstance);

        gameInstances.add(gameInstance);
    }

    public void unregisterGame(GameInstance gameInstance) {
        MinecraftServer.getServerApi().unregisterMiniGameInstance(gameInstance);
        gameInstances.remove(gameInstance);
    }

    public GameInstance getAvailableGameInstance() {
        return gameInstances.stream()
                .filter(GameInstance::acceptPlayers)
                .max(Comparator.comparingInt(gameInstance -> gameInstance.getPlayingPlayers().size()))
                .orElse(null);
    }

    private void registerPlayerMiniGameMoveRequest() {
        MinecraftServer.getGlobalEventHandler()
                .addListener(AsyncPlayerConfigurationEvent.class, event -> {
                    if (event.getRequestedMiniGameInstance() != null) {
                        event.setSpawningInstance(((GameInstance) event.getRequestedMiniGameInstance()));
                    } else {
                        GameInstance gameInstance = getAvailableGameInstance();
                        if (gameInstance != null) {
                            event.setSpawningInstance(gameInstance);
                        } else {
                            event.getPlayer().kick(Component.text("No game instances are available.").color(NamedTextColor.RED));
                        }
                    }

                    event.getPlayer().setRespawnPoint(new Pos(55.5, 101, 52.5));
                });
    }

    /**
     * Hides all the players from players in different game instances
     */
    private void isolatePlayers() {
        MinecraftServer.getGlobalEventHandler()
                .addListener(PlayerSpawnEvent.class, event -> {
                    Player player = event.getPlayer();
                    GameInstance playerGameInstance = (GameInstance) player.getInstance();

                    final PlayerInfoRemovePacket playerRemovePacket = new PlayerInfoRemovePacket(player.getUuid());
                    List<UUID> hiddenPlayers = new ArrayList<>();
                    for (GameInstance gameInstance : gameInstances) {
                        if (gameInstance != playerGameInstance) {
                            hiddenPlayers.addAll(gameInstance.getPlayers().stream().map(Player::getUuid).toList());

                            gameInstance.sendGroupedPacket(playerRemovePacket); // Hide this player from others
                        }
                    }

                    player.sendPacket(new PlayerInfoRemovePacket(hiddenPlayers)); // Hide other players from this player
                })
                .addListener(PlayerChatEvent.class, playerChatEvent -> {
                    GameInstance gameInstance = (GameInstance) playerChatEvent.getPlayer().getInstance();

                    for (GameInstance instance : gameInstances) {
                        if (instance != gameInstance) {
                            playerChatEvent.getRecipients().removeAll(instance.getPlayers());
                        }
                    }
                });
    }

    private void disconnectPlayersWhenMarkedForStop() {
        MinecraftServer.getGlobalEventHandler()
                .addListener(ServerMarkedForStopEvent.class, serverMarkedForStopEvent -> {
                    for (GameInstance gameInstance : gameInstances) {
                        if (gameInstance.acceptPlayers()) { // Only disconnect players from game instances that are accepting players (where the game hasn't started yet)
                            for (Player player : gameInstance.getPlayers()) {
                                player.sendToServer("towerbow"); // Send them to a new server
                            }
                        }
                    }
                });
    }
}
