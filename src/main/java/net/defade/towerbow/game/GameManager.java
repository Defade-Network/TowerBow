package net.defade.towerbow.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class GameManager {
    private static final int MAX_GAME_INSTANCES = 20;

    private final Set<GameInstance> gameInstances = new HashSet<>();

    public GameManager() {
        checkGameInstances();

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            GameInstance gameInstance = getAvailableGameInstance();
            if (gameInstance == null) {
                event.getPlayer().kick(Component.text("No game instances are available.").color(NamedTextColor.RED));
            } else {
                event.setSpawningInstance(gameInstance);
            }
        });
    }

    public void checkGameInstances() {
        int gameInstancesAcceptingPlayers = (int) gameInstances.stream()
                .filter(GameInstance::acceptsPlayers)
                .count();

        while (gameInstancesAcceptingPlayers < 2) {
            createGameInstance();
            gameInstancesAcceptingPlayers++;
        }
    }

    public void createGameInstance() {
        GameInstance gameInstance = new GameInstance(this);
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
}
