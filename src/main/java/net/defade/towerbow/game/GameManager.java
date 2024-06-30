package net.defade.towerbow.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.timer.TaskSchedule;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public class GameManager {
    private static final Path LOBBY_PATH;

    static {
        Path path = null;
        try {
            // List all the files in the console
            path = Paths.get(ClassLoader.getSystemResource("lobby.amethyst").toURI());
        } catch (Exception exception) {
            MinecraftServer.getExceptionManager().handleException(exception);
            System.exit(1);
        }

        LOBBY_PATH = path;
    }

    public static final int MIN_PLAYERS = 4;
    public static final int MAX_PLAYERS = 12;
    private static final int MAX_GAME_INSTANCES = 20;

    private final Set<GameInstance> gameInstances = new CopyOnWriteArraySet<>();

    public GameManager() {
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            GameInstance gameInstance = getAvailableGameInstance();
            if (gameInstance == null) {
                event.getPlayer().kick(Component.text("No game instances are available.").color(NamedTextColor.RED));
            } else {
                event.setSpawningInstance(gameInstance);
            }
        });

        MinecraftServer.getSchedulerManager().scheduleTask(this::checkGameInstances, TaskSchedule.immediate(), TaskSchedule.seconds(2));

        isolatePlayers();
    }

    public void checkGameInstances() {
        int gameInstancesAcceptingPlayers = (int) gameInstances.stream()
                .filter(GameInstance::acceptsPlayers)
                .count();

        while (gameInstancesAcceptingPlayers < 2 && gameInstances.size() < MAX_GAME_INSTANCES) {
            createGameInstance();
            gameInstancesAcceptingPlayers++;
        }
    }

    public void createGameInstance() {
        GameInstance gameInstance = new GameInstance(this, LOBBY_PATH);
        MinecraftServer.getInstanceManager().registerInstance(gameInstance);
        gameInstances.add(gameInstance);

        MinecraftServer.getServerApi().setAllowPlayers(gameInstances.size() < MAX_GAME_INSTANCES);
    }

    public void unregisterGame(GameInstance gameInstance) {
        gameInstances.remove(gameInstance);
    }

    public GameInstance getAvailableGameInstance() {
        return gameInstances.stream()
                .filter(GameInstance::acceptsPlayers)
                .max(Comparator.comparingInt(gameInstance -> gameInstance.getPlayers().size()))
                .orElse(null);
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
}
