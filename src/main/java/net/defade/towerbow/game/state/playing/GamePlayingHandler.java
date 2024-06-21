package net.defade.towerbow.game.state.playing;

import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.game.state.GameStateHandler;
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

    private final GameInstance gameInstance;

    private int tickCounter = 0; // Used to schedule tasks and events like the world border shrinking

    public GamePlayingHandler(GameInstance gameInstance) {
        super(gameInstance);
        this.gameInstance = gameInstance;

        getGameEventNode().getInstanceNode().addListener(InstanceTickEvent.class, instanceTickEvent -> {
            tickCounter++;

            switch (tickCounter) {
                case 5 * 20 -> { // Remove blindness and allow players to move again
                    Potion jumpBoost = new Potion(PotionEffect.JUMP_BOOST, (byte) 1, 30 * 20);
                    gameInstance.getPlayers().forEach(player -> {
                        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(FREEZE_PLAYER_MODIFIER);
                        player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).removeModifier(FREEZE_PLAYER_MODIFIER);

                        player.addEffect(jumpBoost);
                    });
                }
                case 35 * 20 -> { // The players lost their jump boost effect, we can now enable damages
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
                case 10 * 60 * 20 -> // After 10 min, resize the world border
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
        });

        long startTime = System.currentTimeMillis();
        getGameEventNode().getPlayerNode().addListener(
                EventListener.builder(PlayerMoveEvent.class)
                        .expireWhen(playerMoveEvent -> System.currentTimeMillis() + 5000 > startTime)
                        .handler(playerMoveEvent -> playerMoveEvent.setCancelled(true)) // Some players might use hacked clients, so we cancel the event on the server side
                        .build()
        );
    }
}
