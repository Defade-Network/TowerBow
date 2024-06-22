package net.defade.towerbow.game.state;

import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.teams.Team;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.utils.NamespaceID;
import java.util.HashMap;
import java.util.Map;

public class GamePlayingHandler extends GameStateHandler {
    private static final AttributeModifier FREEZE_PLAYER_MODIFIER = new AttributeModifier(NamespaceID.from("defade:freeze_player"), -10000, AttributeOperation.ADD_VALUE);
    private static final int TICKS_BEFORE_WORLD_BORDER_SHRINK = 10 * 60 * 20; // 10 minutes
    private static final int IMMUNITY_TICKS = 30 * 20; // 30 seconds

    private final GameInstance gameInstance;

    private final BossBar bossBar = BossBar.bossBar(
            Component.text(),
            1.0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
    );
    private final Map<Team, Sidebar> teamSidebar = new HashMap<>();

    private PlayingState playingState = PlayingState.IMMOBILE;
    private int tickCounter = 0; // Used to schedule tasks and events like the world border shrinking

    public GamePlayingHandler(GameInstance gameInstance) {
        super(gameInstance);
        this.gameInstance = gameInstance;

        getGameEventNode().getInstanceNode().addListener(InstanceTickEvent.class, instanceTickEvent -> {
            tickCounter++;
            updateBossBar();

            if (playingState == PlayingState.IMMOBILE && tickCounter == 5 * 20) {
                Potion jumpBoost = new Potion(PotionEffect.JUMP_BOOST, (byte) 1, IMMUNITY_TICKS);
                gameInstance.getPlayers().forEach(player -> {
                    player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(FREEZE_PLAYER_MODIFIER);
                    player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).removeModifier(FREEZE_PLAYER_MODIFIER);

                    player.addEffect(jumpBoost);

                    player.showBossBar(bossBar);
                });

                playingState = PlayingState.INVINCIBLE;
                tickCounter = 0; // We reset it to 0 so that we don't have to offset everything by 5 seconds.


                // Remove "starting" text and replace it with immunity timer
                teamSidebar.values().forEach(sidebar -> {
                    sidebar.removeLine("starting");

                    sidebar.createLine(new Sidebar.ScoreboardLine(
                            "bonus_block",
                            getBonusBlockComponent("??:??"), // TODO when bonus blocks will be implemented
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

                return;
            }

            switch (tickCounter) {
                case IMMUNITY_TICKS -> { // The players lost their jump boost effect, we can now enable damages
                    getGameEventNode().getEntityInstanceNode().addChild(CombatMechanics.create());

                    getGameEventNode().getEntityInstanceNode().addListener(PlayerTickEvent.class, playerTickEvent -> {
                        if (playerTickEvent.getPlayer().getPosition().y() < 15) { // TODO: determine right height and damage
                            playerTickEvent.getPlayer().damage(
                                    new Damage(
                                            DamageType.FALL,
                                            null,
                                            null,
                                            null,
                                            1
                                    )
                            );
                        }
                    });

                    playingState = PlayingState.PLAYING;
                }
                case TICKS_BEFORE_WORLD_BORDER_SHRINK ->
                        gameInstance.setWorldBorder(new WorldBorder(50, 0, 0, 0, 0), 60); // Shrink to 50x50 over 60 seconds
            }
        });
    }

    @Override
    public void register() {
        super.register();

        // Give blindness to all players for 5 seconds and don't allow them to move
        Potion blindness = new Potion(PotionEffect.BLINDNESS, (byte) 1, 5 * 20);
        gameInstance.getPlayers().forEach(player -> {
            player.addEffect(blindness);
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(FREEZE_PLAYER_MODIFIER);
            player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).addModifier(FREEZE_PLAYER_MODIFIER);

            player.setFoodSaturation(0); // Disable food saturation
        });

        long startTime = System.currentTimeMillis();
        getGameEventNode().getPlayerNode().addListener(
                EventListener.builder(PlayerMoveEvent.class)
                        .expireWhen(playerMoveEvent -> System.currentTimeMillis() + 5000 > startTime)
                        .handler(playerMoveEvent -> playerMoveEvent.setCancelled(true)) // Some players might use hacked clients, so we cancel the event on the server side
                        .build()
        );

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
                        Component.text("┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈").decoration(TextDecoration.STRIKETHROUGH, true).color(NamedTextColor.DARK_GRAY),
                        7,
                        Sidebar.NumberFormat.blank()
                ));
                sidebar.createLine(new Sidebar.ScoreboardLine(
                        "team_name",
                        Component.text("» ").color(NamedTextColor.GRAY)
                                .append(Component.text("Vous êtes "))
                                .append(team.name()),
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
                        Component.text("┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈").decoration(TextDecoration.STRIKETHROUGH, true).color(NamedTextColor.DARK_GRAY),
                        0,
                        Sidebar.NumberFormat.blank()
                ));

                teamSidebar.put(team, sidebar);
            }

            sidebar.addViewer(player);
        });
    }

    private void updateBossBar() {
        int ticksLeftBeforeBorderShrink = TICKS_BEFORE_WORLD_BORDER_SHRINK - tickCounter;
        int borderShrinkSecondsLeft = Math.max(ticksLeftBeforeBorderShrink, 0) / 20;
        String borderShrinkFormattedTime = String.format("%02d:%02d", borderShrinkSecondsLeft / 60, borderShrinkSecondsLeft % 60);

        String bonusBlockFormattedTime = "??:??"; // TODO when bonus blocks will be implemented

        teamSidebar.values().forEach(sidebar -> {
            sidebar.updateLineContent("bonus_block", getBonusBlockComponent(bonusBlockFormattedTime));
            sidebar.updateLineContent("border", getBorderShrinkComponent(borderShrinkFormattedTime));
        });

        if (tickCounter <= IMMUNITY_TICKS) {
            bossBar.name(Component.text("Invincibilité").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true)
                    .append(Component.text(" (").color(NamedTextColor.GRAY))
                    .append(Component.text((IMMUNITY_TICKS - tickCounter) / 20 + "s").color(NamedTextColor.WHITE))
                    .append(Component.text(")").color(NamedTextColor.GRAY)));

            bossBar.color(BossBar.Color.YELLOW);

            bossBar.progress(1.0f - ((float) tickCounter / IMMUNITY_TICKS));
            return;
        }

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
                        .append(Component.text(" (").color(NamedTextColor.GRAY))
                        .append(Component.text(borderShrinkFormattedTime).color(NamedTextColor.WHITE))
                        .append(Component.text(")").color(NamedTextColor.GRAY))
        );

        bossBar.progress(1.0f - ((float) tickCounter / TICKS_BEFORE_WORLD_BORDER_SHRINK));
    }

    private static Component getBonusBlockComponent(String time) {
        return Component.text("» ").color(NamedTextColor.GRAY)
                .append(Component.text(" Bloc Bonus: ").color(NamedTextColor.WHITE))
                .append(Component.text(time).color(NamedTextColor.RED));
    }

    private static Component getBorderShrinkComponent(String time) {
        return Component.text("» ").color(NamedTextColor.GRAY)
                .append(Component.text(" Bordure: ").color(NamedTextColor.WHITE))
                .append(Component.text(time).color(NamedTextColor.RED));
    }

    private enum PlayingState {
        IMMOBILE,
        INVINCIBLE,
        PLAYING
    }
}
