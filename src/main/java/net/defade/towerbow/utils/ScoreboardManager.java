package net.defade.towerbow.utils;

import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.game.GamePlayHandler;
import net.defade.towerbow.teams.GameTeams;
import net.defade.towerbow.teams.Team;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.scoreboard.Sidebar;

import java.util.HashMap;
import java.util.Map;

import static net.defade.towerbow.game.GamePlayHandler.IMMUNITY_TICKS;
import static net.defade.towerbow.game.GamePlayHandler.TICKS_BEFORE_WORLD_BORDER_SHRINK;

public class ScoreboardManager {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final GameInstance gameInstance;
    private final GamePlayHandler gamePlayHandler;

    private final BossBar bossBar = BossBar.bossBar(
            Component.text(),
            1.0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
    );
    private final Map<Team, Sidebar> teamSidebar = new HashMap<>();

    public ScoreboardManager(GameInstance gameInstance, GamePlayHandler gamePlayHandler) {
        this.gameInstance = gameInstance;
        this.gamePlayHandler = gamePlayHandler;
    }

    public void init() {
        createSidebarScoreboards();
        gamePlayHandler.getGameEventNode().getInstanceNode().addListener(InstanceTickEvent.class, event -> updateScoreboard());
    }

    public void startGame() {
        gameInstance.getPlayers().forEach(player -> player.showBossBar(bossBar));

        // Remove "starting" text and replace it with immunity timer
        teamSidebar.values().forEach(sidebar -> {
            sidebar.removeLine("starting");

            sidebar.createLine(new Sidebar.ScoreboardLine(
                    "bonus_block",
                    getBonusBlockComponent("3:00"),
                    4,
                    Sidebar.NumberFormat.blank()
            ));

            sidebar.createLine(new Sidebar.ScoreboardLine(
                    "border",
                    getBorderShrinkComponent("10:00"),
                    3,
                    Sidebar.NumberFormat.blank()
            ));
        });
    }

    public void updateScoreboard() {
        updateScoreboardTimers();

        if (gamePlayHandler.getPlayingState() != GamePlayHandler.PlayingState.PLAYING) {
            updatePrePlayingScoreboard();
        } else {
            gameInstance.getPlayers().forEach(player -> player.sendActionBar(generateActionBarForPlayer(player)));
        }
    }

    private void updateScoreboardTimers() {
        final int tickCounter = gamePlayHandler.getTickCounter();

        int ticksLeftBeforeBorderShrink = TICKS_BEFORE_WORLD_BORDER_SHRINK - tickCounter;
        int borderShrinkSecondsLeft = Math.max(ticksLeftBeforeBorderShrink, 0) / 20;
        String borderShrinkFormattedTime = String.format("%02d:%02d", borderShrinkSecondsLeft / 60, borderShrinkSecondsLeft % 60);

        int ticksLeftBeforeBonusBlock = gamePlayHandler.getTicksBeforeNextBonusBlock() - tickCounter;
        int bonusBlockSecondsLeft = Math.max(ticksLeftBeforeBonusBlock, 0) / 20;
        String bonusBlockFormattedTime = String.format("%02d:%02d", bonusBlockSecondsLeft / 60, bonusBlockSecondsLeft % 60);

        teamSidebar.values().forEach(sidebar -> {
            sidebar.updateLineContent("bonus_block", getBonusBlockComponent(bonusBlockFormattedTime));
            sidebar.updateLineContent("border", getBorderShrinkComponent(borderShrinkFormattedTime));
        });

        if (ticksLeftBeforeBorderShrink < 0) {
            return; // The world border has already shrunk
        } else if (ticksLeftBeforeBorderShrink == TICKS_BEFORE_WORLD_BORDER_SHRINK / 2 && bossBar.color() != BossBar.Color.YELLOW) {
            bossBar.color(BossBar.Color.YELLOW);
        } else if (ticksLeftBeforeBorderShrink == 60 * 20 && bossBar.color() != BossBar.Color.RED) { // 1 minute
            bossBar.color(BossBar.Color.RED);
        } else if (ticksLeftBeforeBorderShrink == 0) {
            MinecraftServer.getBossBarManager().destroyBossBar(bossBar);
        } else {
            bossBar.color(BossBar.Color.GREEN);
        }


        bossBar.name(
                Component.text("Bordure").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true)
                        .append(Component.text(" (").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)
                                .append(Component.text(borderShrinkFormattedTime).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                                .append(Component.text(")").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)))
        );

        bossBar.progress(1.0f - ((float) tickCounter / TICKS_BEFORE_WORLD_BORDER_SHRINK));
    }

    private void updatePrePlayingScoreboard() {
        final GamePlayHandler.PlayingState playingState = gamePlayHandler.getPlayingState();
        final int tickCounter = gamePlayHandler.getTickCounter();

        bossBar.name(Component.text("Invincibilité").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true)
                .append(Component.text(" (").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text((IMMUNITY_TICKS - tickCounter) / 20 + "s").color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                .append(Component.text(")").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));

        bossBar.color(BossBar.Color.YELLOW);

        bossBar.progress(1.0f - ((float) tickCounter / IMMUNITY_TICKS));

        if (playingState == GamePlayHandler.PlayingState.IMMOBILE) {
            gameInstance.getPlayers().forEach(player -> player.sendActionBar(MM.deserialize("<dark_gray>»</dark_gray> <gray>Préparez vous à monter...</gray> <dark_gray>«</dark_gray>")));
        } else {
            gameInstance.getPlayers().forEach(player -> {
                player.sendActionBar(MM.deserialize("<dark_gray>»</dark_gray> <gray>Montez! Vous êtes invincible.</gray> <dark_gray>«</dark_gray>"));
                gameInstance.sendGroupedPacket(new ParticlePacket(
                        Particle.SOUL_FIRE_FLAME,
                        true,
                        player.getPosition(),
                        new Vec(0, 0, 0),
                        0.07F,
                        1
                ));
                gameInstance.sendGroupedPacket(new ParticlePacket(
                        Particle.ENCHANTED_HIT,
                        true,
                        player.getPosition().add(0,1,0),
                        new Vec(0.3, 0.5, 0.3),
                        0.02F,
                        1
                ));
            });
        }
    }

    private Component generateActionBarForPlayer(Player player) {
        GameTeams gameTeams = gameInstance.getTeams().getGameTeams();
        Component component = Component.text("");

        for (Player playerInTeam : gameInstance.getTeams().getPlayers(gameTeams.firstTeam())) {
            TextColor heartColor = playerInTeam.getGameMode() == GameMode.SPECTATOR ? TextColor.color(26, 26, 26) : TextColor.color(gameTeams.firstTeam().color());
            component = component.append(Component.text("❤ ").color(heartColor));
        }

        component = component
                .append(Component.text("| ").color(NamedTextColor.GRAY))
                .append(Component.text(CombatMechanics.getKills(player) + " kills").color(NamedTextColor.YELLOW))
                .append(Component.text(" | ").color(NamedTextColor.GRAY));

        for (Player playerInTeam : gameInstance.getTeams().getPlayers(gameTeams.secondTeam())) {
            TextColor heartColor = playerInTeam.getGameMode() == GameMode.SPECTATOR ? TextColor.color(26, 26, 26) : TextColor.color(gameTeams.secondTeam().color());
            component = component.append(Component.text("❤ ").color(heartColor));
        }

        return component;
    }

    private void createSidebarScoreboards() {
        gameInstance.getPlayers().forEach(player -> {
            Team team = gameInstance.getTeams().getTeam(player);
            Sidebar sidebar = teamSidebar.get(team);

            if (sidebar == null) {
                sidebar = new Sidebar(
                        Component.text("»").color(NamedTextColor.DARK_GRAY)
                                .append(Component.text(" TOWERBOW").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true))
                                .append(Component.text(" «").color(NamedTextColor.DARK_GRAY))
                );

                sidebar.createLine(new Sidebar.ScoreboardLine(
                        "bar_1",
                        Component.text("                          ").decoration(TextDecoration.STRIKETHROUGH, true).color(NamedTextColor.DARK_GRAY),
                        7,
                        Sidebar.NumberFormat.blank()
                ));
                sidebar.createLine(new Sidebar.ScoreboardLine(
                        "team_name",

                        Component.text("» ").color(NamedTextColor.GRAY)
                                .append(Component.text("Vous êtes ").color(NamedTextColor.WHITE))
                                .append(team.name()).color(TextColor.color(team.color())),
                        6,
                        Sidebar.NumberFormat.blank()
                ));

                sidebar.createLine(new Sidebar.ScoreboardLine(
                        "empty_1",
                        Component.text(""),
                        5,
                        Sidebar.NumberFormat.blank()
                ));

                sidebar.createLine(new Sidebar.ScoreboardLine(
                        "starting",
                        Component.text("  Démarrage...").color(NamedTextColor.WHITE),
                        3,
                        Sidebar.NumberFormat.blank()
                ));

                sidebar.createLine(new Sidebar.ScoreboardLine(
                        "empty_2",
                        Component.text(""),
                        2,
                        Sidebar.NumberFormat.blank()
                ));

                sidebar.createLine(new Sidebar.ScoreboardLine(
                        "server_ip",
                        Component.text("» ").color(NamedTextColor.GRAY)
                                .append(Component.text("defade.net").color(NamedTextColor.YELLOW)),
                        1,
                        Sidebar.NumberFormat.blank()
                ));
                sidebar.createLine(new Sidebar.ScoreboardLine(
                        "bar_2",
                        Component.text("                          ").decoration(TextDecoration.STRIKETHROUGH, true).color(NamedTextColor.DARK_GRAY),
                        0,
                        Sidebar.NumberFormat.blank()
                ));

                teamSidebar.put(team, sidebar);
            }

            sidebar.addViewer(player);
        });
    }

    private static Component getBonusBlockComponent(String time) {
        return Component.text("» ").color(NamedTextColor.GRAY)
                .append(Component.text("Bloc Bonus: ").color(NamedTextColor.WHITE))
                .append(Component.text(time).color(NamedTextColor.RED));
    }

    private static Component getBorderShrinkComponent(String time) {
        return Component.text("» ").color(NamedTextColor.GRAY)
                .append(Component.text("Bordure: ").color(NamedTextColor.WHITE))
                .append(Component.text(time).color(NamedTextColor.RED));
    }
}
