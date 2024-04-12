package net.defade.towerbow.game;

import net.defade.towerbow.utils.GameEventNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.Map;

public class GameStartHandler implements Runnable {
    private static final Map<Integer, Integer> PLAYER_COUNTDOWN = Map.of( // Key: Player count, Value: Countdown
            4, 60,
            5, 50,
            6, 40,
            7, 30,
            8, 20,
            9, 20,
            10, 20,
            11, 10,
            12, 5
    );

    private final GameInstance gameInstance;
    private final GameEventNode eventNode;
    private final Task task;

    private int countdown = Integer.MAX_VALUE;

    private int ticks = 0;

    public GameStartHandler(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
        this.eventNode = new GameEventNode(gameInstance, gameInstance.getEventNode().getNode());

        task = gameInstance.scheduler().scheduleTask(this, TaskSchedule.immediate(), TaskSchedule.tick(1));

        eventNode.getPlayerNode().addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
            Player player = playerSpawnEvent.getPlayer();

            gameInstance.sendMessage(
                    Component.text("✅").color(TextColor.color(0, 170, 0))
                            .append(Component.text(" | ").color(TextColor.color(107, 107, 107)))
                            .append(player.getName()
                                    .append(Component.text(" a rejoint la partie. ")).color(TextColor.color(0, 255, 0)))
                            .append(Component.text("(" + gameInstance.getPlayers().size() + "/12)").color(TextColor.color(157, 157, 157)))
            );

            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(new Pos(0, 1, 0));

            int connectedPlayers = gameInstance.getPlayers().size();
            if(countdown > PLAYER_COUNTDOWN.getOrDefault(connectedPlayers, Integer.MAX_VALUE)) countdown = PLAYER_COUNTDOWN.get(connectedPlayers);
            gameInstance.setAcceptsPlayers(connectedPlayers < 12);
        });

        eventNode.getPlayerNode().addListener(PlayerDisconnectEvent.class, playerDisconnectEvent -> gameInstance.sendMessage(
                Component.text("❌").color(TextColor.color(170, 0, 0))
                        .append(Component.text(" | ").color(TextColor.color(107, 107, 107)))
                        .append(playerDisconnectEvent.getPlayer().getName()
                                .append(Component.text(" a quitté la partie. ")).color(TextColor.color(255, 0, 0)))
                        .append(Component.text("(" + (gameInstance.getPlayers().size() - 1) + "/12)").color(TextColor.color(157, 157, 157)))
        ));
    }

    @Override
    public void run() {
        int connectedPlayers = gameInstance.getPlayers().size();
        if(connectedPlayers <= 1) {
            countdown = Integer.MAX_VALUE;
            return;
        }

        if(ticks == 20) {
            countdown--;
            ticks = 0;

            switch (countdown) {
                case 60, 40, 30, 20, 10, 5, 4, 3, 2, 1 -> gameInstance.sendMessage(
                        Component.text("» ").color(TextColor.color(NamedTextColor.GRAY))
                                .append(Component.text("La partie commence dans ").color(TextColor.color(255, 255, 75)))
                                .append(Component.text(countdown).color(TextColor.color(250, 65, 65)))
                                .append(Component.text(" secondes.").color(TextColor.color(255, 255, 75)).decoration(TextDecoration.BOLD, false))
                );
                case 0 -> {
                    task.cancel();
                    eventNode.unregister();
                }
            }
        }

        ticks++;
    }
}
