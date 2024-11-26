package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
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
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent;
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
                    createExplosion(gameInstance, projectileCollideWithBlockEvent.getCollisionPosition(), 3);
                    entity.scheduleNextTick(Entity::remove);
                })
                .addListener(ProjectileCollideWithEntityEvent.class, projectileCollideWithEntityEvent -> {
                    Entity entity = projectileCollideWithEntityEvent.getEntity();
                    boolean isExplosiveArrow = entity.hasTag(EXPLOSIVE_ARROW) && entity.getTag(EXPLOSIVE_ARROW);
                    entity.setTag(EXPLOSIVE_ARROW, false); // Remove it so that it doesn't explode again

                    if (!isExplosiveArrow) return;
                    createExplosion(gameInstance, projectileCollideWithEntityEvent.getCollisionPosition(), 4);
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

    public void createExplosion(GameInstance gameInstance, Pos position, int radius) {
        gameInstance.sendGroupedPacket(new ParticlePacket(
                Particle.EXPLOSION_EMITTER,
                true,
                position,
                new Vec(0, 0, 0),
                1F,
                1
        ));

        gameInstance.playSound(Sound.sound().type(SoundEvent.BLOCK_TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM).pitch(1F).volume(1.5F).build(), position);
        gameInstance.playSound(Sound.sound().type(SoundEvent.BLOCK_VAULT_BREAK).pitch(0F).volume(1F).build(), position);
        gameInstance.playSound(Sound.sound().type(SoundEvent.ENTITY_GENERIC_EXPLODE).pitch(1F).volume(1F).build(), position);

        // Get all blocks in a radius*radius*radius cube around the hit block
        int blockX = position.blockX();
        int blockY = position.blockY();
        int blockZ = position.blockZ();
        Pos blockPos = new Pos(blockX, blockY, blockZ);

        for (int x = blockX - radius; x <= blockX + radius; x++) {
            for (int y = Math.max(1, blockY - radius); y <= blockY + radius; y++) { // Don't break the floor
                for (int z = blockZ - radius; z <= blockZ + radius; z++) {
                    Block blockType = gameInstance.getBlock(x, y, z);
                    if (blockType == Block.AIR) continue;
                    Pos pos = new Pos(x, y, z);
                    if (pos.distanceSquared(blockPos) > radius * radius) continue; // Only break blocks in a radius*radius*radius sphere

                    gameInstance.setBlock(x, y, z, Block.MOSSY_COBBLESTONE);
                    gameInstance.getWorldHandler().registerBlockDecay(pos, 2 * 1000); // 2 seconds
                }
            }
        }
    }
}
