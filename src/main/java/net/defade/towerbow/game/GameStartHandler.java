package net.defade.towerbow.game;

import net.defade.towerbow.teams.TeamSelectorGUI;
import net.defade.towerbow.utils.GameEventNode;
import net.defade.towerbow.utils.Items;
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
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;

import java.time.Duration;
import java.util.Map;

public class GameStartHandler {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Map<Integer, Integer> PLAYER_COUNTDOWN = Map.of( // Key: Player count, Value: Countdown
            4, 30,
            5, 30,
            6, 20,
            7, 30,
            8, 20,
            9, 30,
            10, 20,
            11, 20,
            12, 10
    );

    private final GameInstance gameInstance;
    private final GameEventNode startEventNode;

    private final TeamSelectorGUI teamSelectorGUI;
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
        this.teamSelectorGUI = new TeamSelectorGUI(gameInstance);

        disableBlockBreaking();

        registerJoinMessages();
        registerLeaveMessages();

        registerGameStartCountdown();

        registerTeamSelector();

        preventPlayersFalling();
    }

    private void disableBlockBreaking() {
        startEventNode.getPlayerNode().addListener(PlayerBlockBreakEvent.class, playerBlockBreakEvent -> {
            playerBlockBreakEvent.setCancelled(true);
        });
    }

    private void registerJoinMessages() {
        startEventNode.getPlayerNode().addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
            GameInstance gameInstance = (GameInstance) playerSpawnEvent.getInstance();
            Player player = playerSpawnEvent.getPlayer();

            // Rules of towerbow message
            player.sendMessage(MM.deserialize(
                    """
                            <st><dark_gray>                </dark_gray></st> <b><gold>TOWERBOW</gold></b> <st><dark_gray>                </dark_gray></st>
                            <yellow>             Inspiré du SkyHigh</yellow>
                            <u><yellow>Règles:</yellow></u>
                            <gray>•</gray> Combats à l'arc!
                            <gray>•</gray> 3 vies!
                            <gray>•</gray> 2 équipes!
                            <gray>•</gray> Blocs infinis!

                            <gray>[</gray><b><light_purple><hover:show_text:'<b><light_purple>BLOC BONUS</light_purple></b>
                            Tirez dessus, donne des bonus.

                            <light_purple><u>Liste des bonus:</u></light_purple>
                             - Heal complet\s
                             - Flèche wall
                             - Foudre contre les adversaires
                             - Flèche explosive
                             - Flèche fumigène'>BLOC BONUS</hover></light_purple></b><gray>] [</gray><b><red><hover:show_text:'<b><red>BORDURES</red></b>
                            Derrière la bordure = dégâts

                            <u><red>Bordure sur les côtés:</red></u>
                             - Statique puis réduit en quelques minutes
                             - Bordure bleue

                            <u><red>Bordure en hauteur (couche Y):</red></u>
                             - La bordure monte toutes les 8s
                             - Bordure rouge'>BORDURES</hover></red></b><gray>] (hover)</gray>
                            <st><dark_gray>                                               </dark_gray></st>"""
            ));

            gameInstance.sendMessage(MM.deserialize(
                    "<gold>🏹 " + player.getUsername() + "</gold><color:#fffb2b> a rejoint la partie.</color> <gray>(" + gameInstance.getPlayers().size() + "/12)</gray>"
            ));

            int connectedPlayers = gameInstance.getPlayers().size();
            if (connectedPlayers >= GameManager.MIN_PLAYERS) {
                tickCountdown = Math.min(tickCountdown, PLAYER_COUNTDOWN.get(connectedPlayers) * 20);
            }

            gameInstance.setAcceptsPlayers(connectedPlayers < GameManager.MAX_PLAYERS);

            player.showBossBar(bossBar);
        });
    }

    private void registerLeaveMessages() {
        startEventNode.getEntityInstanceNode().addListener(RemoveEntityFromInstanceEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) return;
            Instance instance = player.getInstance();

            instance.sendMessage(MM.deserialize(
                    "<color:#aa0000>❌ " + player.getUsername() + "</color> <red>a quitté la partie.</red> <gray>(" + (gameInstance.getPlayers().size() - 1) + "/12)</gray>"
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
                case 60 * 20, 40 * 20, 30 * 20, 20 * 20, 10 * 20, 5 * 20, 4 * 20, 3 * 20, 2 * 20, 1 * 20 -> {
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

                case 0 -> {
                    // Check if all the players are in the same team
                    if (gameInstance.getTeams().firstTeam().getPlayers().size() == connectedPlayers || gameInstance.getTeams().secondTeam().getPlayers().size() == connectedPlayers) {
                        gameInstance.getPlayers().forEach(player -> {
                            player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(7500),Duration.ofMillis(1000)));
                            player.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
                            player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>Une team est vide!</red>"));

                            player.sendMessage(MM.deserialize(
                                    "<dark_red><b>ERREUR!</b></dark_red> <red>Impossible de lancer, une team est vide!</red>"
                            ));

                            player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_PLING).pitch(0F).volume(1F).build(), player.getPosition());
                        });
                        tickCountdown = 15*20;
                    } else {
                        gameInstance.startGame();
                    }
                }
            }
            tickCountdown--;
        });
    }

    private void registerTeamSelector() {
        startEventNode.getPlayerNode()
                .addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
                    Player player = playerSpawnEvent.getPlayer();
                    player.getInventory().addItemStack(Items.TEAM_SELECTOR);
                })
                .addListener(PlayerUseItemEvent.class, playerUseItemEvent -> {
                    Player player = playerUseItemEvent.getPlayer();
                    if (playerUseItemEvent.getItemStack().isSimilar(Items.TEAM_SELECTOR)) {
                        player.openInventory(teamSelectorGUI);
                    }
                });
    }

    private void preventPlayersFalling() {
        startEventNode.getPlayerNode().addListener(PlayerTickEvent.class, playerTickEvent -> {
            if (playerTickEvent.getPlayer().getPosition().y() < 50) {
                playerTickEvent.getPlayer().teleport(new Pos(55.5, 101, 52.5));
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

    public void stopTimer() {
        tickCountdown = Integer.MAX_VALUE;
    }

    public void stop() {
        MinecraftServer.getBossBarManager().destroyBossBar(bossBar);
        startEventNode.unregister();
    }
}
