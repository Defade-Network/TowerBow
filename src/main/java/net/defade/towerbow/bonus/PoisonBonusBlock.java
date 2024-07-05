package net.defade.towerbow.bonus;

import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.teams.GameTeams;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.scoreboard.Team;

import java.time.Duration;

public class PoisonBonusBlock implements BonusBlock {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Potion POISON_POTION = new Potion(
            PotionEffect.POISON,
            (byte) 1,
            10 * 20 // 5 seconds
    );

    @Override
    public void onHit(Player shooter) {
        GameInstance gameInstance = (GameInstance) shooter.getInstance();
        Team playerTeam = shooter.getTeam();

        Team oppositeTeam = gameInstance.getTeams().firstTeam() == playerTeam
                ? gameInstance.getTeams().secondTeam()
                : gameInstance.getTeams().firstTeam();

        oppositeTeam.getPlayers().forEach(player -> {
            if (CombatMechanics.isDead(player)) return;
            player.addEffect(POISON_POTION);

            gameInstance.sendGroupedPacket(new ParticlePacket(
                    Particle.DAMAGE_INDICATOR,
                    true,
                    player.getPosition(),
                    new Vec(0.2, 1, 0.2),
                    0.5F,
                    50
            ));

            player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(3500),Duration.ofMillis(500)));
            player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<dark_red><b>BLOC BONUS!</b></dark_red>"));
            player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>Les adversaires vous infligent poison!</red>"));
        });
    }

    @Override
    public void registerMechanics(GameInstance gameInstance) {

    }
}
