package net.defade.towerbow.game.state;

import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.game.GameInstance;
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
import net.minestom.server.utils.NamespaceID;

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

    private boolean starting = true;
    private int tickCounter = 0; // Used to schedule tasks and events like the world border shrinking

    public GamePlayingHandler(GameInstance gameInstance) {
        super(gameInstance);
        this.gameInstance = gameInstance;

        getGameEventNode().getInstanceNode().addListener(InstanceTickEvent.class, instanceTickEvent -> {
            tickCounter++;
            updateBossBar();

            switch (tickCounter) {
                case 5 * 20 -> { // Remove blindness and allow players to move again
                    if (!starting) return;

                    Potion jumpBoost = new Potion(PotionEffect.JUMP_BOOST, (byte) 1, IMMUNITY_TICKS);
                    gameInstance.getPlayers().forEach(player -> {
                        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(FREEZE_PLAYER_MODIFIER);
                        player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).removeModifier(FREEZE_PLAYER_MODIFIER);

                        player.addEffect(jumpBoost);
                    });

                    starting = false;
                    tickCounter = 0; // We reset it to 0 so that we don't have to offset everything by 5 seconds.
                }
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

            player.showBossBar(bossBar);

            player.setFoodSaturation(0); // Disable food saturation
        });

        long startTime = System.currentTimeMillis();
        getGameEventNode().getPlayerNode().addListener(
                EventListener.builder(PlayerMoveEvent.class)
                        .expireWhen(playerMoveEvent -> System.currentTimeMillis() + 5000 > startTime)
                        .handler(playerMoveEvent -> playerMoveEvent.setCancelled(true)) // Some players might use hacked clients, so we cancel the event on the server side
                        .build()
        );
    }

    private void updateBossBar() {
        if (tickCounter <= IMMUNITY_TICKS) {
            bossBar.name(
                    Component.text("Invincibilité").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true)
                            .append(Component.text(" (").color(NamedTextColor.GRAY))
                            .append(Component.text((IMMUNITY_TICKS - tickCounter) / 20 + "s").color(NamedTextColor.WHITE))
                            .append(Component.text(")").color(NamedTextColor.GRAY))
            );

            bossBar.color(BossBar.Color.YELLOW);

            bossBar.progress(1.0f - ((float) tickCounter / IMMUNITY_TICKS));
            return;
        }

        int ticksLeftBeforeShrink = TICKS_BEFORE_WORLD_BORDER_SHRINK - tickCounter;

        if (ticksLeftBeforeShrink < 0) {
            return; // The world border has already shrunk
        } else if (ticksLeftBeforeShrink == TICKS_BEFORE_WORLD_BORDER_SHRINK / 2 && bossBar.color() != BossBar.Color.YELLOW) {
            bossBar.color(BossBar.Color.YELLOW);
        } else if (ticksLeftBeforeShrink == 60 * 20 && bossBar.color() != BossBar.Color.RED) { // 1 minute
            bossBar.color(BossBar.Color.RED);
        } else if (ticksLeftBeforeShrink == 0) {
            MinecraftServer.getBossBarManager().destroyBossBar(bossBar);
        } else {
            bossBar.color(BossBar.Color.GREEN);
        }

        int secondsLeft = ticksLeftBeforeShrink / 20;
        int formattedMinutes = secondsLeft / 60;
        int formattedSeconds = secondsLeft % 60;

        String formattedTime = String.format("%02d:%02d", formattedMinutes, formattedSeconds);

        bossBar.name(
                Component.text("Bordure").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true)
                        .append(Component.text(" (").color(NamedTextColor.GRAY))
                        .append(Component.text(formattedTime).color(NamedTextColor.WHITE))
                        .append(Component.text(")").color(NamedTextColor.GRAY))
        );

        bossBar.progress(1.0f - ((float) tickCounter / TICKS_BEFORE_WORLD_BORDER_SHRINK));
    }
}
