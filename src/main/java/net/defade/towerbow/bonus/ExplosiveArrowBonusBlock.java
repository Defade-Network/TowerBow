package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

import java.time.Duration;

public class ExplosiveArrowBonusBlock implements BonusBlock {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Tag<Boolean> HAS_EXPLOSIVE_ARROW = Tag.Boolean("has_explosive_arrow"); // Only used for the particle effect

    @Override
    public void onHit(Player shooter) {
        shooter.setTag(HAS_EXPLOSIVE_ARROW, true);

        shooter.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3500), Duration.ofMillis(500)));
        shooter.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
        shooter.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<dark_gray>»</dark_gray> <b><red>"
            + "EXPLOSIVE ARROW</red></b> <dark_gray>«</dark_gray>"));

        shooter.eventNode()
            .addListener(
                EventListener.builder(EntityShootEvent.class)
                    .handler(entityShootEvent -> {
                        shooter.setTag(HAS_EXPLOSIVE_ARROW, false);
                        registerArrowEvent(entityShootEvent.getProjectile());
                    })
                    .expireCount(1)
                    .build()
            )
            .addListener(
                EventListener.builder(PlayerTickEvent.class)
                    .handler(playerTickEvent -> {
                        playerTickEvent.getInstance().sendGroupedPacket(new ParticlePacket(
                            Particle.DUST.withProperties(new Color(255, 0, 0), 1),
                            true,
                            playerTickEvent.getPlayer().getPosition().add(0, 0.5, 0),
                            new Vec(0.2, 0.5, 0.2),
                            0.02F,
                            1
                        ));
                    })
                    .expireWhen(unused -> shooter.getTag(HAS_EXPLOSIVE_ARROW) == null || !shooter.getTag(HAS_EXPLOSIVE_ARROW))
                    .build()
            );
    }

    private void registerArrowEvent(Entity projectile) {
        projectile.eventNode()
            .addListener(
                EventListener.builder(ProjectileCollideWithBlockEvent.class)
                    .handler(projectileCollideWithBlockEvent -> {
                        createExplosion((GameInstance) projectileCollideWithBlockEvent.getInstance(),
                            projectileCollideWithBlockEvent.getCollisionPosition(), 4);
                        projectileCollideWithBlockEvent.getEntity().remove();
                    })
                    .expireWhen(unused -> projectile.isRemoved())
                    .expireCount(1)
                    .build()
            )
            .addListener(
                EventListener.builder(ProjectileCollideWithEntityEvent.class)
                    .handler(projectileCollideWithEntityEvent -> {
                        createExplosion((GameInstance) projectileCollideWithEntityEvent.getInstance(),
                            projectileCollideWithEntityEvent.getCollisionPosition(), 4);
                        projectileCollideWithEntityEvent.getEntity().remove();
                    })
                    .expireWhen(unused -> projectile.isRemoved())
                    .expireCount(1)
                    .build()
            )
            .addListener(
                EventListener.builder(EntityTickEvent.class)
                    .handler(entityTickEvent -> {
                        Entity entity = entityTickEvent.getEntity();
                        entityTickEvent.getInstance().sendGroupedPacket(new ParticlePacket(
                            Particle.DUST.withProperties(new Color(255, 0, 0), 2.5F),
                            true,
                            entity.getPosition(),
                            new Vec(0, 0, 0),
                            0F,
                            1
                        ));
                    })
                    .expireWhen(unused -> projectile.isRemoved())
                    .build()
            );
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
                    if (pos.distanceSquared(blockPos) > radius * radius)
                        continue; // Only break blocks in a radius*radius*radius sphere

                    gameInstance.setBlock(x, y, z, Block.MOSSY_COBBLESTONE);
                    gameInstance.getWorldHandler().registerBlockDecay(pos, 2 * 1000); // 2 seconds
                }
            }
        }
    }
}
