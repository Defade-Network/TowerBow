package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import java.time.Duration;

public class WallArrowBonusBlock implements BonusBlock {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Tag<Boolean> HAS_WALL_ARROW = Tag.Boolean("has_wall_arrow"); // Only used for the particle effect

    @Override
    public void onHit(Player shooter) {
        shooter.setTag(HAS_WALL_ARROW, true);

                shooter.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3500), Duration.ofMillis(500)));
        shooter.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
        shooter.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<dark_gray>»</dark_gray> <b><red>"
                + "WALL ARROW</red></b> <dark_gray>«</dark_gray>"));

        shooter.eventNode()
                .addListener(
                        EventListener.builder(EntityShootEvent.class)
                                .handler(entityShootEvent -> {
                                    shooter.setTag(HAS_WALL_ARROW, false);
                                    registerArrowEvent(entityShootEvent.getProjectile());

                                    shooter.getInstance().scheduler().scheduleTask(() -> {
                                        createWall(entityShootEvent.getProjectile().getPosition(), (GameInstance) shooter.getInstance());
                                        entityShootEvent.getProjectile().scheduleNextTick(Entity::remove);
                                    }, TaskSchedule.millis(200), TaskSchedule.stop());
                                })
                                .expireCount(1)
                                .build()
                )
                .addListener(
                        EventListener.builder(PlayerTickEvent.class)
                                .handler(playerTickEvent -> {
                                    shooter.getInstance().sendGroupedPacket(new ParticlePacket(
                                            Particle.FIREWORK,
                                            true,
                                            shooter.getPosition().add(0, 0.5, 0),
                                            new Vec(0.2, 0.5, 0.2),
                                            0.02F,
                                            1
                                    ));
                                })
                                .expireWhen(unused -> !shooter.hasTag(HAS_WALL_ARROW) || !shooter.getTag(HAS_WALL_ARROW))
                                .build()
                );
    }

    private void registerArrowEvent(Entity arrow) {
        arrow.eventNode()
                .addListener(
                        EventListener.builder(EntityTickEvent.class)
                                .handler(entityTickEvent -> {
                                    entityTickEvent.getInstance().sendGroupedPacket(new ParticlePacket(
                                            Particle.CRIT,
                                            true,
                                            arrow.getPosition(),
                                            new Vec(0.25, 0.25, 0.25),
                                            0.25F,
                                            20
                                    ));
                                })
                                .expireWhen(unused -> !arrow.hasTag(HAS_WALL_ARROW) || !arrow.getTag(HAS_WALL_ARROW))
                                .build()
                );
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
                    if (gameInstance.getNearbyEntities(new Pos(x, y, z), 3).stream().anyMatch(entity -> entity.getEntityType() == EntityType.PLAYER))
                        continue; //Don't create blocks inside of a player

                    gameInstance.setBlock(x, y, z, Block.MOSSY_COBBLESTONE);
                    gameInstance.getWorldHandler().registerBlockDecay(new Pos(x, y, z), 13 * 1000 + (long) (Math.random() * 2000)); // 15 seconds
                }
            }
        }
    }
}

