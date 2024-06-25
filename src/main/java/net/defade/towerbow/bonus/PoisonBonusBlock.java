package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.teams.GameTeams;
import net.defade.towerbow.teams.Team;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;

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
        Team playerTeam = gameInstance.getTeams().getTeam(shooter);
        GameTeams gameTeams = gameInstance.getTeams().getGameTeams();

        Team oppositeTeam;
        if (playerTeam == gameTeams.firstTeam()) {
            oppositeTeam = gameTeams.secondTeam();
        } else {
            oppositeTeam = gameTeams.firstTeam();
        }

        gameInstance.getTeams().getPlayers(oppositeTeam).forEach(player -> {
            player.addEffect(POISON_POTION);
            player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(3500),Duration.ofMillis(500)));
            player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<dark_red><b>BLOC BONUS!</b></dark_red>"));
            player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<red>Les adversaires vous infligent poison!</red>"));
        });
    }

    @Override
    public void registerMechanics(GameInstance gameInstance) {

    }
}
