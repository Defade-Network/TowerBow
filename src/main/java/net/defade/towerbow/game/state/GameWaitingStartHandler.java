package net.defade.towerbow.game.state;

import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import java.util.Map;

public class GameWaitingStartHandler extends GameStateHandler {
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

    private final BossBar bossBar = BossBar.bossBar(
            Component.text(""),
            1.0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
    );

    private int countdown = Integer.MAX_VALUE;
    private int ticks = 0;

    public GameWaitingStartHandler(GameInstance gameInstance) {
        super(gameInstance);

        registerJoinMessages();
        registerLeaveMessages();

        registerGameStartCountdown();
    }

    private void registerJoinMessages() {
        getGameEventNode().getPlayerNode().addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
            GameInstance gameInstance = (GameInstance) playerSpawnEvent.getInstance();
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

            player.showBossBar(bossBar);
        });
    }

    private void registerLeaveMessages() {
        getGameEventNode().getPlayerNode().addListener(PlayerDisconnectEvent.class, playerDisconnectEvent -> {
            Instance instance = playerDisconnectEvent.getPlayer().getInstance();

            instance.sendMessage(
                    Component.text("❌").color(TextColor.color(170, 0, 0))
                            .append(Component.text(" | ").color(TextColor.color(107, 107, 107)))
                            .append(playerDisconnectEvent.getPlayer().getName()
                                    .append(Component.text(" a quitté la partie. ")).color(TextColor.color(255, 0, 0)))
                            .append(Component.text("(" + (instance.getPlayers().size() - 1) + "/12)").color(TextColor.color(157, 157, 157)))
            );
        });
    }

    private void registerGameStartCountdown() {
        getGameEventNode().getInstanceNode().addListener(InstanceTickEvent.class, instanceTickEvent -> {
            GameInstance gameInstance = (GameInstance) instanceTickEvent.getInstance();

            updateBossBar(PLAYER_COUNTDOWN.getOrDefault(gameInstance.getPlayers().size(), Integer.MAX_VALUE));

            int connectedPlayers = gameInstance.getPlayers().size();
            if(connectedPlayers <= 1) {
                countdown = Integer.MAX_VALUE;
                ticks = 0;
                return;
            }

            if(ticks % 20 == 0) {
                countdown--;

                switch (countdown) {
                    case 60, 40, 30, 20, 10, 5, 4, 3, 2, 1 -> gameInstance.sendMessage(
                            Component.text("» ").color(TextColor.color(NamedTextColor.GRAY))
                                    .append(Component.text("La partie commence dans ").color(TextColor.color(255, 255, 75)))
                                    .append(Component.text(countdown).color(TextColor.color(250, 65, 65)))
                                    .append(Component.text(" secondes.").color(TextColor.color(255, 255, 75)).decoration(TextDecoration.BOLD, false))
                    );
                    case 0 -> gameInstance.startGame();
                }
            }

            ticks++;
        });
    }

    private void updateBossBar(int countdownTime) {
        if (countdown == Integer.MAX_VALUE) { // We're not counting down
            bossBar.name(
                    Component.text("En attente de joueurs... ")
                            .color(NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, true)
            );
        } else {
            bossBar.name(
                    Component.text("Démarrage... ")
                            .color(NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, true)
                            .append(Component.text("(").color(NamedTextColor.GRAY))
                            .append(Component.text(countdown + " secondes").color(NamedTextColor.WHITE))
                            .append(Component.text(")").color(NamedTextColor.GRAY)
                            ));

            bossBar.progress(1.0f - ((float) (ticks) / ((countdownTime * 20))));
        }
    }

    @Override
    public void unregister() {
        super.unregister();

        MinecraftServer.getBossBarManager().destroyBossBar(bossBar);
    }
}
