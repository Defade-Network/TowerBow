package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.sound.SoundEvent;

import java.time.Duration;

public class StrikeBonusBlock implements BonusBlock {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public void onHit(Player shooter) {
        GameInstance gameInstance = (GameInstance) shooter.getInstance();
        Team playerTeam = shooter.getTeam();

        Team oppositeTeam = gameInstance.getTeams().firstTeam() == playerTeam
                ? gameInstance.getTeams().secondTeam()
                : gameInstance.getTeams().firstTeam();

        oppositeTeam.getPlayers().forEach(player -> {

            new Entity(EntityType.LIGHTNING_BOLT).setInstance(gameInstance, player.getPosition());

            if(player.getHealth() <= 8) {
                player.setHealth(1);
            } else {
                player.damage(
                        new Damage(
                                DamageType.OUT_OF_WORLD, // Bypass armor protection
                                null,
                                null,
                                null,
                                8
                        )
                );
            }

            gameInstance.sendGroupedPacket(new ParticlePacket(
                    Particle.CLOUD,
                    true,
                    player.getPosition(),
                    new Vec(0.2, 1, 0.2),
                    0.5F,
                    50
            ));

            player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(3500),Duration.ofMillis(500)));
            player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<dark_red><b>FOUDRE!</b></dark_red>"));
            player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>Les adversaires envoient la foudre!</red>"));
        });
        gameInstance.getPlayers().forEach(players -> gameInstance.playSound(Sound.sound().type(SoundEvent.ENTITY_LIGHTNING_BOLT_THUNDER).pitch(1F).volume(0.5F).build(), players.getPosition()));
    }

    @Override
    public void registerMechanics(GameInstance gameInstance) {

    }
}
