package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.utils.Items;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.scoreboard.Team;

import java.time.Duration;

public class HealBonusBlock implements BonusBlock {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public void onHit(Player shooter) {
        GameInstance gameInstance = (GameInstance) shooter.getInstance();
        Team playerTeam = shooter.getTeam();

        playerTeam.getPlayers().forEach(player -> {

            if (player.getHealth() >= 20) {
                player.getInventory().addItemStack(Items.GOLDEN_APPLE);
            }

            player.setHealth(20);
            player.setAdditionalHearts(player.getAdditionalHearts() + 8);

            gameInstance.sendGroupedPacket(new ParticlePacket(
                    Particle.HEART,
                    true,
                    player.getPosition(),
                    new Vec(0.2, 1, 0.2),
                    0.02F,
                    20
            ));

            player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(1500),Duration.ofMillis(500)));
            player.sendTitlePart(TitlePart.TITLE, MM.deserialize(" "));
            player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<light_purple><b>FULL HEAL!</b></light_purple>"));

        });
    }

    @Override
    public void registerMechanics(GameInstance gameInstance) {

    }
}
