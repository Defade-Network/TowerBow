package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

import java.time.Duration;

public class SmokeArrowBonusBlock implements BonusBlock {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Tag<Boolean> HAS_SMOKE_ARROW = Tag.Boolean("has_smoke_arrow"); // Only used for the particle effect

    private static final Tag<Boolean> HAS_ARROW_HIT = Tag.Boolean("has_arrow_hit");
    private static final int SMOKE_TICKS_TIME = 15 * 20; // 15 seconds

    private static final Potion BLINDNESS = new Potion(
        PotionEffect.BLINDNESS,
        (byte) 1,
        40
    );

    @Override
    public void onHit(Player shooter) {
        shooter.setTag(HAS_SMOKE_ARROW, true);

        shooter.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3500), Duration.ofMillis(500)));
        shooter.sendTitlePart(TitlePart.TITLE, MM.deserialize(""));
        shooter.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<dark_gray>»</dark_gray> <b><red>"
            + "SMOKE ARROW</red></b> <dark_gray>«</dark_gray>"));

        shooter.eventNode()
            .addListener(
                EventListener.builder(EntityShootEvent.class)
                    .handler(entityShootEvent -> {
                        Entity arrow = entityShootEvent.getProjectile();
                        shooter.setTag(HAS_SMOKE_ARROW, false);

                        arrow.eventNode()
                            .addListener(EventListener.builder(ProjectileCollideWithBlockEvent.class)
                                .handler(projectileCollideWithBlockEvent -> {
                                    arrow.setTag(HAS_ARROW_HIT, true);
                                    registerArrowTickEvent(projectileCollideWithBlockEvent.getEntity());
                                })
                                .expireCount(1)
                                .expireWhen(unused -> arrow.getTag(HAS_ARROW_HIT) != null && arrow.getTag(HAS_ARROW_HIT))
                                .build())
                            .addListener(EventListener.builder(ProjectileCollideWithEntityEvent.class)
                                .handler(projectileCollideWithEntityEvent -> {
                                    arrow.setTag(HAS_ARROW_HIT, true);

                                    if (projectileCollideWithEntityEvent.getTarget() instanceof Player damaged) {
                                        if (shooter.getTeam() != damaged.getTeam()) {
                                            applyBlindnessToSinglePlayer((Player) projectileCollideWithEntityEvent.getTarget());
                                        }
                                    }
                                })
                                .expireCount(1)
                                .expireWhen(unused -> arrow.getTag(HAS_ARROW_HIT) != null && arrow.getTag(HAS_ARROW_HIT))
                                .build());
                    })
                    .expireCount(1)
                    .build()
            )
            .addListener(
                EventListener.builder(PlayerTickEvent.class)
                    .handler(playerTickEvent -> {
                        playerTickEvent.getInstance().sendGroupedPacket(new ParticlePacket(
                            Particle.SMOKE,
                            true,
                            playerTickEvent.getPlayer().getPosition(),
                            new Vec(0.2, 0, 0.2),
                            0.01F,
                            5
                        ));
                    })
                    .expireWhen(unused -> shooter.getTag(HAS_SMOKE_ARROW) == null || !shooter.getTag(HAS_SMOKE_ARROW))
                    .build()
            );
    }

    private void registerArrowTickEvent(Entity arrow) {
        arrow.eventNode()

            .addListener(
                EventListener.builder(EntityTickEvent.class)
                    .handler(entityTickEvent -> {
                        Entity entity = entityTickEvent.getEntity();

                        if (entity.getAliveTicks() >= SMOKE_TICKS_TIME) {
                            entity.scheduleNextTick(Entity::remove);
                            return;
                        }

                        // Spawn smoke particles and give everyone in a 4 block radius blindness
                        entity.getInstance().sendGroupedPacket(new ParticlePacket(
                            Particle.LARGE_SMOKE,
                            true,
                            entity.getPosition(),
                            new Vec(3, 3, 3),
                            0.02F,
                            10
                        ));
                        entity.getInstance().sendGroupedPacket(new ParticlePacket(
                            Particle.CAMPFIRE_COSY_SMOKE,
                            true,
                            entity.getPosition(),
                            new Vec(0.2, 0.2, 0.2),
                            0.1F,
                            6
                        ));
                        entity.getInstance().sendGroupedPacket(new ParticlePacket(
                            Particle.ASH,
                            true,
                            entity.getPosition(),
                            new Vec(3, 3, 3),
                            1F,
                            50
                        ));

                        entity.getInstance().playSound(Sound.sound().type(SoundEvent.ENTITY_BREEZE_IDLE_GROUND).pitch(0F).volume(0.3F).build(), entity.getPosition());
                        for (Player player : ((GameInstance) entity.getInstance()).getAlivePlayers()) {
                            if (player.getDistanceSquared(entity) <= 6 * 6 && player.getGameMode() == GameMode.SURVIVAL) {
                                player.addEffect(BLINDNESS);
                                player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(500), Duration.ofMillis(500)));
                                player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<dark_gray><b>SMOKE ARROW!</b></dark_gray>"));
                                player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<gray>Une flèche adverse vous aveugle!</gray>"));
                            }
                        }
                    })
                    .expireWhen(unused -> arrow.isRemoved())
                    .build()
            );
    }

    private void applyBlindnessToSinglePlayer(Player player) {
        Potion blindness = new Potion(
            PotionEffect.BLINDNESS,
            (byte) 1,
            12 * 20
        );

        player.addEffect(blindness);
        player.playSound(Sound.sound().type(SoundEvent.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR).pitch(0F).volume(0.3F).build(), player.getPosition());
        player.playSound(Sound.sound().type(SoundEvent.ENTITY_GUARDIAN_AMBIENT).pitch(0F).volume(1.5F).build(), player.getPosition());
        player.playSound(Sound.sound().type(SoundEvent.ENTITY_GUARDIAN_HURT).pitch(0F).volume(1.5F).build(), player.getPosition());
        player.playSound(Sound.sound().type(SoundEvent.ENTITY_ENDERMAN_HURT).pitch(0F).volume(1F).build(), player.getPosition());

        player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(8000), Duration.ofMillis(500)));
        player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<dark_gray><b>SMOKE ARROW!</b></dark_gray>"));
        player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<gray>Une flèche adverse vous aveugle!</gray>"));
    }
}
