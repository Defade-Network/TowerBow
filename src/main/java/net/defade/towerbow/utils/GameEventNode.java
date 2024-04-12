package net.defade.towerbow.utils;

import net.defade.towerbow.game.GameInstance;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerEvent;

public class GameEventNode {
    private final EventNode<Event> parentNode;
    private final EventNode<Event> childNode = EventNode.all("game_event_node");

    private final EventNode<PlayerEvent> playerNode;

    public GameEventNode(GameInstance gameInstance, EventNode<Event> parentNode) {
        this.parentNode = parentNode;
        this.playerNode = EventNode.type("game_event_node_player", EventFilter.PLAYER, ((playerEvent, player) -> gameInstance.getPlayers().contains(player)));

        this.parentNode.addChild(childNode);

        this.childNode.addChild(playerNode);
    }

    public EventNode<Event> getNode() {
        return childNode;
    }

    public EventNode<PlayerEvent> getPlayerNode() {
        return playerNode;
    }

    public void unregister() {
        parentNode.removeChild(childNode);
    }
}
