package net.defade.towerbow.game;

import net.defade.towerbow.map.AmethystMapSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.timer.TaskSchedule;
import java.util.Comparator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;

public class GameManager {
    public static final int MIN_PLAYERS = 4;
    public static final int MAX_PLAYERS = 12;
    private static final int MAX_GAME_INSTANCES = 20;

    private final Timer timer = new Timer();

    private final AmethystMapSource mapSource = new AmethystMapSource();
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

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkGameInstances();
            }
        }, 0, 2000);
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
        GameInstance gameInstance = new GameInstance(this, mapSource.getRandomMap());
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
