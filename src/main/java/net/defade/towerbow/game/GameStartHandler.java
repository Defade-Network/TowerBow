package net.defade.towerbow.game;

import net.defade.towerbow.utils.GameEventNode;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;

import java.time.Duration;
import java.util.Map;

public class GameStartHandler {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Map<Integer, Integer> PLAYER_COUNTDOWN = Map.of( // Key: Player count, Value: Countdown
            4, 20,
            5, 20,
            6, 20,
            7, 20,
            8, 20,
            9, 20,
            10, 20,
            11, 10,
            12, 5
    );

    private final GameInstance gameInstance;
    private final GameEventNode startEventNode;
    private final BossBar bossBar = BossBar.bossBar(
            Component.text(""),
            1.0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
    );

    private int tickCountdown = Integer.MAX_VALUE;

    public GameStartHandler(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
        this.startEventNode = new GameEventNode(gameInstance.getEventNode());

        registerJoinMessages();
        registerLeaveMessages();

        registerGameStartCountdown();
    }

    private void registerJoinMessages() {
        startEventNode.getPlayerNode().addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
            GameInstance gameInstance = (GameInstance) playerSpawnEvent.getInstance();
            Player player = playerSpawnEvent.getPlayer();

            gameInstance.sendMessage(MM.deserialize(
                    "<gold>🏹 " + player.getUsername() + "</gold><color:#fffb2b> a rejoint la partie.</color> <gray>(" + gameInstance.getPlayers().size() + "/12)</gray>"
            ));

            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(new Pos(0, 1, 0));

            int connectedPlayers = gameInstance.getPlayers().size();
            if (connectedPlayers >= GameManager.MIN_PLAYERS) {
                tickCountdown = Math.min(tickCountdown, PLAYER_COUNTDOWN.get(connectedPlayers) * 20);
            }

            gameInstance.setAcceptsPlayers(connectedPlayers < 12);

            player.showBossBar(bossBar);
        });
    }

    private void registerLeaveMessages() {
        startEventNode.getPlayerNode().addListener(PlayerDisconnectEvent.class, playerDisconnectEvent -> {
            Instance instance = playerDisconnectEvent.getPlayer().getInstance();

            instance.sendMessage(MM.deserialize(
                    "<color:#aa0000>❌ " + playerDisconnectEvent.getPlayer().getUsername() + "</color> <red>a quitté la partie.</red> <gray>(" + (gameInstance.getPlayers().size() - 1) + "/12)</gray>"
            ));

            if (instance.getPlayers().size() - 1 < GameManager.MIN_PLAYERS) {
                tickCountdown = Integer.MAX_VALUE;
            }
        });
    }

    private void registerGameStartCountdown() {
        startEventNode.getInstanceNode().addListener(InstanceTickEvent.class, instanceTickEvent -> {
            GameInstance gameInstance = (GameInstance) instanceTickEvent.getInstance();

            updateBossBar(PLAYER_COUNTDOWN.getOrDefault(gameInstance.getPlayers().size(), 1));

            int connectedPlayers = gameInstance.getPlayers().size();
            if (connectedPlayers < GameManager.MIN_PLAYERS) {
                return;
            }

            switch (tickCountdown) {
                case 60*20, 40*20, 30*20, 20*20, 10*20, 5*20, 4*20, 3*20, 2*20, 1*20 -> {
                    gameInstance.sendMessage(
                            Component.text("» ").color(TextColor.color(NamedTextColor.GRAY))
                                    .append(Component.text("La partie commence dans ").color(TextColor.color(255, 255, 75)))
                                    .append(Component.text(tickCountdown / 20).color(TextColor.color(250, 65, 65)))
                                    .append(Component.text(" secondes.").color(TextColor.color(255, 255, 75)).decoration(TextDecoration.BOLD, false))
                    );

                    gameInstance.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(2000),Duration.ofMillis(500)));
                    gameInstance.sendTitlePart(TitlePart.TITLE, MM.deserialize(" "));
                    gameInstance.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<yellow>" + tickCountdown / 20 + "</yellow>"));


                    gameInstance.getPlayers().forEach(players -> gameInstance.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_HAT).pitch(1F).volume(0.5F).build(), players.getPosition()));
                }

                case 1 -> gameInstance.startGame();
            }
            if (tickCountdown != 0) {
                tickCountdown--;
            }
        });
    }

    private void updateBossBar(int countdownTime) {
        if (gameInstance.getPlayers().size() < GameManager.MIN_PLAYERS) {
            bossBar.name(
                    Component.text("En attente de joueurs... ")
                            .color(NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, true)
            );
            bossBar.progress(1.0f);
        } else {
            bossBar.name(
                    Component.text("Démarrage... ")
                            .color(NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, true)
                            .append(Component.text("(").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                            .append(Component.text((tickCountdown + 20) / 20 + "s").color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                            .append(Component.text(")").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)
                            ));

            bossBar.progress((float) tickCountdown / (countdownTime * 20));
        }
    }
    public void stop() {
        MinecraftServer.getBossBarManager().destroyBossBar(bossBar);
    }
}
