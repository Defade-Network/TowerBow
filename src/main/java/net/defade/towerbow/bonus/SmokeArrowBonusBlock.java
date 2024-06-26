package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

import java.time.Duration;

public class SmokeArrowBonusBlock implements BonusBlock {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Tag<Boolean> SMOKE_ARROW = Tag.Boolean("smoke_arrow");

    private static final Tag<Long> IMPACT_TIME = Tag.Long("impact_time"); // The time when the arrow hit the block
    private static final int SMOKE_TICKS_TIME = 15 * 20; // 15 seconds

    @Override
    public void onHit(Player shooter) {
        shooter.setTag(SMOKE_ARROW, true);
    }

    @Override
    public void registerMechanics(GameInstance gameInstance) {
        gameInstance.getEventNode().getEntityInstanceNode()
                .addListener(EntityShootEvent.class, entityShootEvent -> {
                    boolean isSmokeArrow = entityShootEvent.getEntity().hasTag(SMOKE_ARROW) && entityShootEvent.getEntity().getTag(SMOKE_ARROW);
                    entityShootEvent.getProjectile().setTag(SMOKE_ARROW, isSmokeArrow);

                    entityShootEvent.getEntity().setTag(SMOKE_ARROW, false);
                })
                .addListener(ProjectileCollideWithBlockEvent.class, projectileCollideWithBlockEvent -> {
                    Entity projectile = projectileCollideWithBlockEvent.getEntity();
                    boolean isSmokeArrow = projectile.hasTag(SMOKE_ARROW) && projectile.getTag(SMOKE_ARROW);
                    projectile.setTag(SMOKE_ARROW, false);

                    if (!isSmokeArrow) return;

                    projectile.setTag(IMPACT_TIME, projectile.getAliveTicks());
                })
                .addListener(EntityTickEvent.class, entityTickEvent -> {
                    Entity entity = entityTickEvent.getEntity();
                    if (!entity.hasTag(IMPACT_TIME)) return;

                    long impactTime = entity.getTag(IMPACT_TIME);
                    if (entity.getAliveTicks() - impactTime >= SMOKE_TICKS_TIME) {
                        entity.removeTag(SMOKE_ARROW);
                        return;
                    }

                    // Spawn smoke particles and give everyone in a 4 block radius blindness
                    Potion blindness = new Potion(
                            PotionEffect.BLINDNESS,
                            (byte) 1,
                            40
                    );

                    gameInstance.sendGroupedPacket(new ParticlePacket(
                            Particle.LARGE_SMOKE,
                            true,
                            entity.getPosition(),
                            new Vec(3, 3, 3),
                            0.02F,
                            20
                    ));
                    gameInstance.sendGroupedPacket(new ParticlePacket(
                            Particle.CAMPFIRE_COSY_SMOKE,
                            true,
                            entity.getPosition(),
                            new Vec(0.2, 0.2, 0.2),
                            0.1F,
                            10
                    ));
                    gameInstance.sendGroupedPacket(new ParticlePacket(
                            Particle.ASH,
                            true,
                            entity.getPosition(),
                            new Vec(3, 3, 3),
                            1F,
                            50
                    ));
                    for (Player player : gameInstance.getPlayers()) {
                        if (player.getDistanceSquared(entity) <= 4 * 4) {
                            player.addEffect(blindness);
                            player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(1000),Duration.ofMillis(500)));
                            player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<dark_gray><b>SMOKE ARROW!</b></dark_gray>"));
                            player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<gray>Une flèche adverse vous aveugle!</gray>"));
                            player.playSound(Sound.sound().type(SoundEvent.ENTITY_BREEZE_IDLE_GROUND).pitch(0F).volume(0.5F).build(), player.getPosition());
                        }
                    }
                });
    }
}
