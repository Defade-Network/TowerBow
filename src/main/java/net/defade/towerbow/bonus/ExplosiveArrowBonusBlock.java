package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.map.WorldHandler;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

public class ExplosiveArrowBonusBlock implements BonusBlock {
    private static final Tag<Boolean> EXPLOSIVE_ARROW = Tag.Boolean("explosive_arrow");

    @Override
    public void onHit(Player shooter) {
        shooter.setTag(EXPLOSIVE_ARROW, true);
    }

    @Override
    public void registerMechanics(GameInstance gameInstance) {
        gameInstance.getEventNode().getEntityInstanceNode()
                .addListener(EntityShootEvent.class, entityShootEvent -> {
                    boolean isExplosiveArrow = entityShootEvent.getEntity().hasTag(EXPLOSIVE_ARROW) && entityShootEvent.getEntity().getTag(EXPLOSIVE_ARROW);
                    entityShootEvent.getProjectile().setTag(EXPLOSIVE_ARROW, isExplosiveArrow);

                    entityShootEvent.getEntity().setTag(EXPLOSIVE_ARROW, false);
                })
                .addListener(ProjectileCollideWithBlockEvent.class, projectileCollideWithBlockEvent -> {
                    Entity entity = projectileCollideWithBlockEvent.getEntity();
                    boolean isExplosiveArrow = entity.hasTag(EXPLOSIVE_ARROW) && entity.getTag(EXPLOSIVE_ARROW);
                    entity.setTag(EXPLOSIVE_ARROW, false); // Remove it so that it doesn't explode again

                    if (!isExplosiveArrow) return;
                    gameInstance.sendGroupedPacket(new ParticlePacket(
                            Particle.EXPLOSION_EMITTER,
                            true,
                            entity.getPosition(),
                            new Vec(0, 0, 0),
                            1F,
                            1
                    ));

                    gameInstance.playSound(Sound.sound().type(SoundEvent.BLOCK_TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM).pitch(1F).volume(1.5F).build(), entity.getPosition());
                    gameInstance.playSound(Sound.sound().type(SoundEvent.BLOCK_VAULT_BREAK).pitch(0F).volume(1F).build(), entity.getPosition());
                    gameInstance.playSound(Sound.sound().type(SoundEvent.ENTITY_GENERIC_EXPLODE).pitch(1F).volume(1F).build(), entity.getPosition());



                    // Get all blocks in a 5x5x5 cube around the hit block TODO: do a sphere check
                    int blockX = projectileCollideWithBlockEvent.getCollisionPosition().blockX();
                    int blockY = projectileCollideWithBlockEvent.getCollisionPosition().blockY();
                    int blockZ = projectileCollideWithBlockEvent.getCollisionPosition().blockZ();

                    for (int x = blockX - 2; x <= blockX + 2; x++) {
                        for (int y = Math.max(1, blockY - 2); y <= blockY + 2; y++) { // Don't break the floor
                            for (int z = blockZ - 2; z <= blockZ + 2; z++) {
                                Block blockType = gameInstance.getBlock(x, y, z);
                                if (blockType == Block.AIR) continue;

                                gameInstance.setBlock(x, y, z, Block.MOSSY_COBBLESTONE);
                                WorldHandler worldHandler = (WorldHandler) gameInstance.generator();
                                // Should never be null
                                if (worldHandler != null) worldHandler.registerBlockDecay(new Pos(x, y, z), 2 * 1000); // 2 seconds
                            }
                        }
                    }
                    entity.scheduleNextTick(Entity::remove);
                })
                .addListener(EntityTickEvent.class, entityTickEvent -> {
                    Entity entity = entityTickEvent.getEntity();
                    if (entity.hasTag(EXPLOSIVE_ARROW) && entity.getTag(EXPLOSIVE_ARROW)) {
                        if (entity.getEntityType() == EntityType.ARROW) {
                            gameInstance.sendGroupedPacket(new ParticlePacket(
                                    Particle.DUST.withProperties(new Color(255,0,0),2.5F),
                                    true,
                                    entity.getPosition(),
                                    new Vec(0, 0, 0),
                                    0F,
                                    1
                            ));
                        } else if (entity.getEntityType() == EntityType.PLAYER) {
                            gameInstance.sendGroupedPacket(new ParticlePacket(
                                    Particle.DUST.withProperties(new Color(255,0,0),1),
                                    true,
                                    entity.getPosition().add(0, 0.5, 0),
                                    new Vec(0.2, 0.5, 0.2),
                                    0.02F,
                                    1
                            ));
                        }

                    }

                });

    }
}
