package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.teams.GameTeams;
import net.defade.towerbow.teams.Team;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;

public class PoisonBonusBlock implements BonusBlock {
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
        });
    }

    @Override
    public void registerMechanics(GameInstance gameInstance) {

    }
}
