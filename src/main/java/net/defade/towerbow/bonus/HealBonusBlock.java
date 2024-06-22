package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.teams.Team;
import net.minestom.server.entity.Player;

public class HealBonusBlock implements BonusBlock {
    @Override
    public void onHit(Player shooter) {
        GameInstance gameInstance = (GameInstance) shooter.getInstance();
        Team playerTeam = gameInstance.getTeams().getTeam(shooter);

        gameInstance.getTeams().getPlayers(playerTeam).forEach(player -> {
            player.setHealth(20);
            player.setAdditionalHearts(player.getAdditionalHearts() + 8);
        });
    }

    @Override
    public void registerMechanics(GameInstance gameInstance) {

    }
}
