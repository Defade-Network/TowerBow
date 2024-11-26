package net.defade.towerbow.game;

import net.defade.towerbow.bonus.BonusBlockManager;
import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.utils.GameEventNode;
import net.defade.towerbow.utils.ScoreboardManager;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Vec;
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
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.NamespaceID;

import java.time.Duration;

public class GamePlayHandler {
    public static final int TICKS_BEFORE_WORLD_BORDER_SHRINK = 8 * 60 * 20; // 8 minutes
    public static final int IMMUNITY_TICKS = 20 * 20; // 20 seconds
    public static final int MIN_TICKS_BEFORE_BLOCK_BONUS_SPAWN = 20 * 20; // 20 seconds
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final AttributeModifier FREEZE_PLAYER_MODIFIER = new AttributeModifier(NamespaceID.from("defade:freeze_player"), -10000, AttributeOperation.ADD_VALUE);

    private final GameInstance gameInstance;
    private final GameEventNode gameEventNode;

    private final BonusBlockManager bonusBlockManager;
    private final ScoreboardManager scoreboardManager;

    private PlayingState playingState = PlayingState.IMMOBILE;
    private int tickCounter = 0; // Used to schedule tasks and events like the world border shrinking
    private int ticksBeforeNextBonusBlock = 70 * 20; // The first bonus block will spawn after 1m10s
    private int horizontalBorderHeight = 10;

    public GamePlayHandler(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
        this.gameEventNode = new GameEventNode(gameInstance.getEventNode(), false);
        this.bonusBlockManager = new BonusBlockManager(gameInstance, this);
        this.scoreboardManager = new ScoreboardManager(gameInstance, this);
    }

    public void start() {
        gameEventNode.register();
        scoreboardManager.init();

        gameInstance.getPlayers().forEach(Player::closeInventory);

        immobilizePlayers();
        registerTickEvents();
    }

    private void immobilizePlayers() {
        // Give blindness to all players for 5 seconds and don't allow them to move
        Potion blindness = new Potion(PotionEffect.BLINDNESS, (byte) 1, 5 * 20);
        gameInstance.getPlayers().forEach(player -> {
            player.addEffect(blindness);
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(FREEZE_PLAYER_MODIFIER);
            player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).addModifier(FREEZE_PLAYER_MODIFIER);
            player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(0);
        });

        long startTime = System.currentTimeMillis();
        gameEventNode.getPlayerNode().addListener(
                EventListener.builder(PlayerMoveEvent.class)
                        .expireWhen(playerMoveEvent -> System.currentTimeMillis() + 5000 > startTime)
                        .handler(playerMoveEvent -> playerMoveEvent.setCancelled(true)) // Some players might use hacked clients, so we cancel the event on the server side
                        .build()
        );
    }

    private void registerTickEvents() {
        gameEventNode.getInstanceNode().addListener(InstanceTickEvent.class, instanceTickEvent -> {
            tickCounter++;

            if (playingState == PlayingState.IMMOBILE && tickCounter == 5 * 20) {
                Potion jumpBoost = new Potion(PotionEffect.JUMP_BOOST, (byte) 2, IMMUNITY_TICKS);
                Potion speed = new Potion(PotionEffect.SPEED, (byte) 0, IMMUNITY_TICKS);
                gameInstance.getPlayers().forEach(player -> {

                    player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(FREEZE_PLAYER_MODIFIER);
                    player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).removeModifier(FREEZE_PLAYER_MODIFIER);
                    player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).setBaseValue(4.5);

                    player.addEffect(jumpBoost);
                    player.addEffect(speed);

                    // Starting sound
                    player.playSound(Sound.sound().type(SoundEvent.ENTITY_ENDER_DRAGON_GROWL).pitch(1.2F).volume(0.5F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_PLING).pitch(2.0F).volume(0.5F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_PLING).pitch(1.0F).volume(0.5F).build(), player.getPosition());
                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_PLING).pitch(0.0F).volume(0.5F).build(), player.getPosition());

                    player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(250),Duration.ofMillis(1500),Duration.ofMillis(250)));
                    player.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
                    player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<yellow>Montez vite!</yellow>"));
                });

                playingState = PlayingState.INVINCIBLE;
                tickCounter = 0; // We reset it to 0 so that we don't have to offset everything by 5 seconds.

                scoreboardManager.startGame();

                return;
            }

            switch (tickCounter) {
                case IMMUNITY_TICKS -> { // The players lost their jump boost effect, we can now enable damages
                    gameEventNode.getEntityInstanceNode().addChild(CombatMechanics.create(gameInstance));

                    // Invincibility lost sound
                    gameInstance.getPlayers().forEach(player -> {
                        player.playSound(Sound.sound().type(SoundEvent.ITEM_TRIDENT_THUNDER).pitch(1F).volume(0.5F).build(), player.getPosition());
                        player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(250),Duration.ofMillis(1500),Duration.ofMillis(250)));
                        player.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
                        player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<gray>Période d'invincibilité terminée!</gray>"));
                    });

                    gameEventNode.getEntityInstanceNode().addListener(PlayerTickEvent.class, playerTickEvent -> {
                        horizontalBorderHeight = 25 + (tickCounter / (5 * 20) ); // +1Y / 5s
                        double playerHeight = playerTickEvent.getPlayer().getPosition().y();
                        Player player = playerTickEvent.getPlayer();

                        //Send warning message to the player & show the Y border
                        if (playerHeight < (horizontalBorderHeight + 16) && playerHeight >= horizontalBorderHeight + 6) {
                            player.sendPacket(new ParticlePacket(
                                    Particle.DUST.withProperties(new Color(255,0,0),1F),
                                    true,
                                    player.getPosition().withY(horizontalBorderHeight),
                                    new Vec(2, 0, 2),
                                    0F,
                                    20
                            ));
                        } else if (playerHeight < (horizontalBorderHeight + 6) && playerHeight >= (horizontalBorderHeight)) {

                            player.sendPacket(new ParticlePacket(
                                    Particle.DUST.withProperties(new Color(255,0,0),1F),
                                    true,
                                    player.getPosition().withY(horizontalBorderHeight),
                                    new Vec(3, 0, 3),
                                    0F,
                                    40
                            ));
                            if (tickCounter % 40 == 1 && player.getGameMode() == GameMode.SURVIVAL) {
                                player.playSound(Sound.sound().type(SoundEvent.ENTITY_GUARDIAN_ATTACK).pitch(0.7F).volume(1F).build(), playerTickEvent.getPlayer().getPosition());

                                player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(1000),Duration.ofMillis(750)));
                                player.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
                                player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>Montez! Vous allez suffoquer.</red>"));
                            }
                        } else if (playerHeight < horizontalBorderHeight) {
                            if (player.getGameMode() == GameMode.SURVIVAL) {

                                if (tickCounter % 20 == 1) { // Triggered every 1sec
                                    player.damage(
                                            new Damage(
                                                    DamageType.MAGIC,
                                                    null,
                                                    null,
                                                    null,
                                                    3
                                            )
                                    );
                                    gameInstance.sendGroupedPacket(new ParticlePacket(
                                            Particle.DAMAGE_INDICATOR,
                                            true,
                                            player.getPosition(),
                                            new Vec(0.1, 0.2, 0.1),
                                            0.5F,
                                            30
                                    ));

                                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM).pitch(0.7F).volume(0.4F).build(), playerTickEvent.getPlayer().getPosition());
                                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_VAULT_BREAK).pitch(0F).volume(0.5F).build(), playerTickEvent.getPlayer().getPosition());
                                }

                                /*
                                Triggered every tick
                                 */

                                player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2200), Duration.ofMillis(750)));
                                player.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
                                player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>Vous êtes <distance> blocks sous la bordure!</red>",
                                        Placeholder.component("distance", Component.text(horizontalBorderHeight - (int) playerHeight)))
                                );

                                player.sendPacket(new ParticlePacket(
                                        Particle.DUST.withProperties(new Color(150, 0, 0), 1F),
                                        true,
                                        player.getPosition().withY(Math.clamp(playerHeight, 4, horizontalBorderHeight - 4)),
                                        new Vec(3, 3, 3),
                                        0.05F,
                                        40
                                ));
                                gameInstance.sendGroupedPacket(new ParticlePacket(
                                        Particle.DUST.withProperties(new Color(255, 0, 0), 1.5F),
                                        true,
                                        player.getPosition().withY(horizontalBorderHeight),
                                        new Vec(4, 0, 4),
                                        0F,
                                        50
                                ));

                            } else { // The player is in spectator (dead), show the Y border particles only to him
                                player.sendPacket(new ParticlePacket(
                                        Particle.DUST.withProperties(new Color(255, 0, 0), 1F),
                                        true,
                                        player.getPosition().withY(horizontalBorderHeight),
                                        new Vec(4, 0, 4),
                                        0F,
                                        80
                                        ));
                                player.sendPacket(new ParticlePacket(
                                        Particle.DUST.withProperties(new Color(150, 0, 0), 1F),
                                        true,
                                        player.getPosition(),
                                        new Vec(3, Math.min(playerHeight - 1, 3), 3),
                                        0.15F,
                                        40
                                ));
                            }
                        }
                    });

                    playingState = PlayingState.PLAYING;
                }
                case TICKS_BEFORE_WORLD_BORDER_SHRINK -> {
                    gameInstance.setWorldBorder(new WorldBorder((int) (gameInstance.getWorldBorder().diameter() / 5) * 2, gameInstance.getWorldBorder().centerX(), gameInstance.getWorldBorder().centerZ(), 0, 0), 160); // Shrinking over 160 seconds
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
                // Add 1 minute minus ~5s per minute passed, capped at min_ticks
                ticksBeforeNextBonusBlock += Math.max((60 * 20) - (tickCounter / 15), MIN_TICKS_BEFORE_BLOCK_BONUS_SPAWN);

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

    public GameEventNode getGameEventNode() {
        return gameEventNode;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public int getHorizontalBorderHeight() {
        return horizontalBorderHeight;
    }

    public int getTicksBeforeNextBonusBlock() {
        return ticksBeforeNextBonusBlock;
    }

    public PlayingState getPlayingState() {
        return playingState;
    }

    public void stop() {
        gameEventNode.unregister();
    }

    public enum PlayingState {
        IMMOBILE,
        INVINCIBLE,
        PLAYING
    }
}
