package net.defade.towerbow.game.state;

import net.defade.towerbow.bonus.BonusBlockManager;
import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.teams.GameTeams;
import net.defade.towerbow.teams.Team;
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
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
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
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.NamespaceID;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class GamePlayingHandler extends GameStateHandler {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final AttributeModifier FREEZE_PLAYER_MODIFIER = new AttributeModifier(NamespaceID.from("defade:freeze_player"), -10000, AttributeOperation.ADD_VALUE);
    private static final int TICKS_BEFORE_WORLD_BORDER_SHRINK = 10 * 60 * 20; // 10 minutes
    private static final int IMMUNITY_TICKS = 20 * 20; // 20 seconds

    private final GameInstance gameInstance;
    private final BonusBlockManager bonusBlockManager;

    private final BossBar bossBar = BossBar.bossBar(
            Component.text(),
            1.0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
    );
    private final Map<Team, Sidebar> teamSidebar = new HashMap<>();

    private PlayingState playingState = PlayingState.IMMOBILE;
    private int tickCounter = 0; // Used to schedule tasks and events like the world border shrinking
    private int ticksBeforeNextBonusBlock = 3 * 60 * 20; // The first bonus block will spawn after 3 minutes

    public GamePlayingHandler(GameInstance gameInstance) {
        super(gameInstance);
        this.gameInstance = gameInstance;
        this.bonusBlockManager = new BonusBlockManager(gameInstance);

        getGameEventNode().getInstanceNode().addListener(InstanceTickEvent.class, instanceTickEvent -> {
            tickCounter++;
            updateScoreboard();

            if (playingState == PlayingState.IMMOBILE && tickCounter == 5 * 20) {
                Potion jumpBoost = new Potion(PotionEffect.JUMP_BOOST, (byte) 2, IMMUNITY_TICKS);
                gameInstance.getPlayers().forEach(player -> {

                    player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(FREEZE_PLAYER_MODIFIER);
                    player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).removeModifier(FREEZE_PLAYER_MODIFIER);
                    player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(4.5);

                    player.addEffect(jumpBoost);

                    player.showBossBar(bossBar);

                    // Starting sound
                    player.playSound(Sound.sound().type(SoundEvent.ENTITY_ENDER_DRAGON_GROWL).pitch(1.2F).volume(0.5F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_PLING).pitch(2.0F).volume(1F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_PLING).pitch(1.0F).volume(1F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_PLING).pitch(0.0F).volume(1F).build(), player.getPosition());

                    player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(250),Duration.ofMillis(1500),Duration.ofMillis(250)));
                    player.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
                    player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<yellow>Montez vite!</yellow>"));
                });

                playingState = PlayingState.INVINCIBLE;
                tickCounter = 0; // We reset it to 0 so that we don't have to offset everything by 5 seconds.


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

                return;
            }

            switch (tickCounter) {
                case IMMUNITY_TICKS -> { // The players lost their jump boost effect, we can now enable damages
                    getGameEventNode().getEntityInstanceNode().addChild(CombatMechanics.create(gameInstance));

                    // Invincibility lost sound
                    gameInstance.getPlayers().forEach(player -> {
                        player.playSound(Sound.sound().type(SoundEvent.ITEM_TRIDENT_THUNDER).pitch(1F).volume(0.5F).build(), player.getPosition());
                        player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(250),Duration.ofMillis(1500),Duration.ofMillis(250)));
                        player.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
                        player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<gray>Période d'invincibilité terminée!</gray>"));
                    });

                    getGameEventNode().getEntityInstanceNode().addListener(PlayerTickEvent.class, playerTickEvent -> {
                        if (playerTickEvent.getPlayer().getPosition().y() < 15) { // TODO: determine right height and damage
                            playerTickEvent.getPlayer().damage(
                                    new Damage( // TODO: Damage the player 2 hearts / 2s
                                            DamageType.FALL,
                                            null,
                                            null,
                                            null,
                                            1

                                    )
                            );

                            playerTickEvent.getPlayer().sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(2000),Duration.ofMillis(750)));
                            playerTickEvent.getPlayer().sendTitlePart(TitlePart.TITLE, MM.deserialize("<dark_red><b>MONTEZ VITE!!</b></dark_red>"));
                            playerTickEvent.getPlayer().sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>Vous êtes trop bas!</red>"));
                        }
                    });

                    playingState = PlayingState.PLAYING;
                }
                case TICKS_BEFORE_WORLD_BORDER_SHRINK -> {
                        gameInstance.setWorldBorder(new WorldBorder(50, 0, 0, 0, 0), 60); // Shrink to 50x50 over 60 seconds
                        // Border shrinking sound & message
                        gameInstance.getPlayers().forEach(player -> {
                            player.playSound(Sound.sound().type(SoundEvent.ENTITY_ELDER_GUARDIAN_CURSE).pitch(0F).volume(0.5F).build(), player.getPosition());
                            player.sendMessage(MM.deserialize(
                                    "<dark_red>\uD83C\uDFF9 <b>BORDURE!</b></dark_red> <red>La bordure réduit jusqu'au centre!</red>"
                            ));
                        });
                }
            }

            if (tickCounter == ticksBeforeNextBonusBlock) {
                ticksBeforeNextBonusBlock += 60 * 20; // Add 1 minute

                bonusBlockManager.spawnBonusBlock();
                gameInstance.sendMessage(MM.deserialize(
                        "<dark_purple>\uD83C\uDFF9 <b>BLOC BONUS!</b></dark_purple> <light_purple>Un bloc bonus est apparu!</light_purple>"
                ));
                gameInstance.getPlayers().forEach(player -> {
                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE).pitch(1F).volume(0.5F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.ENTITY_EVOKER_PREPARE_SUMMON).pitch(2F).volume(0.5F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER).pitch(1F).volume(0.5F).build(), player.getPosition());
                });
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
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(0);

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

    private void updateScoreboard() {
        int ticksLeftBeforeBorderShrink = TICKS_BEFORE_WORLD_BORDER_SHRINK - tickCounter;
        int borderShrinkSecondsLeft = Math.max(ticksLeftBeforeBorderShrink, 0) / 20;
        String borderShrinkFormattedTime = String.format("%02d:%02d", borderShrinkSecondsLeft / 60, borderShrinkSecondsLeft % 60);

        int ticksLeftBeforeBonusBlock = ticksBeforeNextBonusBlock - tickCounter;
        int bonusBlockSecondsLeft = Math.max(ticksLeftBeforeBonusBlock, 0) / 20;
        String bonusBlockFormattedTime = String.format("%02d:%02d", bonusBlockSecondsLeft / 60, bonusBlockSecondsLeft % 60);

        teamSidebar.values().forEach(sidebar -> {
            sidebar.updateLineContent("bonus_block", getBonusBlockComponent(bonusBlockFormattedTime));
            sidebar.updateLineContent("border", getBorderShrinkComponent(borderShrinkFormattedTime));
        });

        if (playingState == PlayingState.PLAYING) gameInstance.getPlayers().forEach(player -> player.sendActionBar(generateActionBarForPlayer(player)));

        if (tickCounter <= IMMUNITY_TICKS) {
            bossBar.name(Component.text("Invincibilité").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true)
                    .append(Component.text(" (").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                    .append(Component.text((IMMUNITY_TICKS - tickCounter) / 20 + "s").color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                    .append(Component.text(")").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));

            bossBar.color(BossBar.Color.YELLOW);

            bossBar.progress(1.0f - ((float) tickCounter / IMMUNITY_TICKS));

            gameInstance.getPlayers().forEach(player -> player.sendActionBar(MM.deserialize("<gray>Montez! Vous êtes invincible.</gray>")));
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
                        .append(Component.text(" (").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)
                        .append(Component.text(borderShrinkFormattedTime).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                        .append(Component.text(")").color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)))
        );

        bossBar.progress(1.0f - ((float) tickCounter / TICKS_BEFORE_WORLD_BORDER_SHRINK));
    }

    private Component generateActionBarForPlayer(Player player) {
        GameTeams gameTeams = gameInstance.getTeams().getGameTeams();
        Component component = Component.text("");

        for (Player playerInTeam : gameInstance.getTeams().getPlayers(gameTeams.firstTeam())) {
            TextColor heartColor = playerInTeam.getGameMode() == GameMode.SPECTATOR ? TextColor.color(26,26,26) : TextColor.color(gameTeams.firstTeam().color());
            component = component.append(Component.text("❤ ").color(heartColor));
        }

        component = component
                .append(Component.text("| ").color(NamedTextColor.GRAY))
                .append(Component.text(player.getTag(CombatMechanics.PLAYER_KILLS) + " kills").color(NamedTextColor.YELLOW))
                .append(Component.text(" | ").color(NamedTextColor.GRAY));

        for (Player playerInTeam : gameInstance.getTeams().getPlayers(gameTeams.secondTeam())) {
            TextColor heartColor = playerInTeam.getGameMode() == GameMode.SPECTATOR ? TextColor.color(26,26,26) : TextColor.color(gameTeams.secondTeam().color());
            component = component.append(Component.text("❤ ").color(heartColor));
        }

        return component;
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

    private enum PlayingState {
        IMMOBILE,
        INVINCIBLE,
        PLAYING
    }
}
