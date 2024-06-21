package net.defade.towerbow.game.state;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.game.state.playing.GamePlayingHandler;

import java.util.function.Function;

public enum GameState {
    WAITING_START(GameWaitingStartHandler::new),
    PLAYING(GamePlayingHandler::new),
    FINISHED(gameInstance -> null);

    private final Function<GameInstance, GameStateHandler> stateHandlerSupplier;

    GameState(Function<GameInstance, GameStateHandler> stateHandlerSupplier) {
        this.stateHandlerSupplier = stateHandlerSupplier;
    }

    /**
     * Creates a new {@link GameStateHandler} for the given {@link GameInstance}.
     *
     * @param gameInstance the game instance
     * @return the new state handler
     */
    public GameStateHandler createStateHandler(GameInstance gameInstance) {
        return stateHandlerSupplier.apply(gameInstance);
    }
}
