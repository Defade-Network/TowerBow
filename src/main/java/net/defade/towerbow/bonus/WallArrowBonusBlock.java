package net.defade.towerbow.bonus;

import io.github.togar2.pvp.entity.projectile.Arrow;
import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

public class WallArrowBonusBlock implements BonusBlock {
    private static final Tag<Boolean> WALL_ARROW = Tag.Boolean("wall_arrow");

    @Override
    public void onHit(Player shooter) {
        shooter.setTag(WALL_ARROW, true);
    }

    @Override
    public void registerMechanics(GameInstance gameInstance) {
        gameInstance.getEventNode().getEntityInstanceNode()
                .addListener(EntityShootEvent.class, entityShootEvent -> {
                    if (!(entityShootEvent.getProjectile() instanceof Arrow arrow)) return;

                    Player player = (Player) entityShootEvent.getEntity();

                    boolean isWallArrow = player.hasTag(WALL_ARROW) && player.getTag(WALL_ARROW);
                    arrow.setTag(WALL_ARROW, isWallArrow);

                    if (!isWallArrow) return;
                    player.setTag(WALL_ARROW, false);

                    gameInstance.scheduler().scheduleTask(() -> {
                        createWall(arrow.getPosition(), gameInstance);
                        arrow.scheduleNextTick(Entity::remove);

                        arrow.setTag(WALL_ARROW, false);
                    }, TaskSchedule.millis(200), TaskSchedule.stop());
                })
                .addListener(EntityTickEvent.class, entityTickEvent -> {
                    Entity entity = entityTickEvent.getEntity();
                    if (entity.hasTag(WALL_ARROW) && entity.getTag(WALL_ARROW)) {
                        if (entity.getEntityType() == EntityType.ARROW) {
                            gameInstance.sendGroupedPacket(new ParticlePacket(
                                    Particle.CRIT,
                                    true,
                                    entity.getPosition(),
                                    new Vec(0.25, 0.25, 0.25),
                                    0.25F,
                                    20
                            ));
                        } else if (entity.getEntityType() == EntityType.PLAYER) {
                            gameInstance.sendGroupedPacket(new ParticlePacket(
                                    Particle.FIREWORK,
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

    private void createWall(Pos position, GameInstance gameInstance) {
        gameInstance.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_HAT).pitch(1F).volume(1.5F).build(), position);
        gameInstance.playSound(Sound.sound().type(SoundEvent.BLOCK_STONE_PLACE).pitch(1F).volume(1.5F).build(), position);
        gameInstance.playSound(Sound.sound().type(SoundEvent.BLOCK_TRIAL_SPAWNER_PLACE).pitch(0F).volume(1F).build(), position);

        gameInstance.sendGroupedPacket(new ParticlePacket(
                Particle.CRIT,
                true,
                position,
                new Vec(2, 2, 2),
                0.02F,
                500
        ));

        gameInstance.sendGroupedPacket(new ParticlePacket(
                Particle.FIREWORK,
                true,
                position,
                new Vec(1.5F, 1.5F, 1.5F),
                0.3F,
                500
        ));

        int blockX = position.blockX();
        int blockY = position.blockY();
        int blockZ = position.blockZ();

        for (int x = blockX - 2; x <= blockX + 2; x++) {
            for (int y = Math.max(1, blockY - 2); y <= blockY + 2; y++) { // Don't break the floor
                for (int z = blockZ - 2; z <= blockZ + 2; z++) {
                    Block blockType = gameInstance.getBlock(x, y, z);
                    if (blockType != Block.AIR) continue;
                    if (gameInstance.getNearbyEntities(new Pos(x, y ,z), 3).stream().anyMatch(entity -> entity.getEntityType() == EntityType.PLAYER)) continue; //Don't create blocks inside of a player

                    gameInstance.setBlock(x, y, z, Block.MOSSY_COBBLESTONE);
                    gameInstance.getWorldHandler().registerBlockDecay(new Pos(x, y, z), 13 * 1000 + (long) (Math.random() * 2000)); // 15 seconds
                }
            }
        }
    }
}

