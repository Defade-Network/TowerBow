package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.minestom.server.entity.Player;

public interface BonusBlock {
    void onHit(Player shooter);

    /**
     * Registers the mechanics of the bonus block.
     */
    void registerMechanics(GameInstance gameInstance);
}
