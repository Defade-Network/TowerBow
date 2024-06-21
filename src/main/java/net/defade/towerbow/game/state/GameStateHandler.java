package net.defade.towerbow.game.state;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.utils.GameEventNode;

public abstract class GameStateHandler {
    private final GameEventNode gameEventNode;

    public GameStateHandler(GameInstance gameInstance) {
        this(new GameEventNode(gameInstance.getEventNode(), false));
    }

    public GameStateHandler(GameEventNode gameEventNode) {
        this.gameEventNode = gameEventNode;
    }

    protected GameEventNode getGameEventNode() {
        return gameEventNode;
    }

    public void register() {
        gameEventNode.register();
    }

    public void unregister() {
        gameEventNode.unregister();
    }
}
