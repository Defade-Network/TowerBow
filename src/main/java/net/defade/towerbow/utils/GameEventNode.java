package net.defade.towerbow.utils;

import net.defade.towerbow.game.GameInstance;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;

public class GameEventNode {
    private static final EventFilter<EntityInstanceEvent, Entity> ENTITY_INSTANCE_FILTER = EventFilter.from(EntityInstanceEvent.class, Entity.class, EntityInstanceEvent::getEntity);

    private final GameInstance gameInstance;

    private final EventNode<Event> parentNode;
    private final EventNode<Event> childNode = EventNode.all("game_event_node");

    private final EventNode<InstanceEvent> instanceNode;
    private final EventNode<EntityInstanceEvent> entityInstanceNode;
    private final EventNode<PlayerEvent> playerNode;

    public GameEventNode(GameEventNode parent) {
        this(parent.gameInstance, parent.childNode);
    }

    public GameEventNode(GameInstance gameInstance, EventNode<Event> parentNode) {
        this.gameInstance = gameInstance;
        this.parentNode = parentNode;

        this.instanceNode = EventNode.type("game_event_node_instance", EventFilter.INSTANCE, ((event, instance) -> gameInstance == instance));
        this.entityInstanceNode = EventNode.type("game_event_node_entity_instance", ENTITY_INSTANCE_FILTER, ((event, entity) -> gameInstance == entity.getInstance()));
        this.playerNode = EventNode.type("game_event_node_player", EventFilter.PLAYER, ((playerEvent, player) -> gameInstance == player.getInstance()));

        this.parentNode.addChild(childNode);

        this.childNode.addChild(instanceNode);
        this.childNode.addChild(entityInstanceNode);
        this.childNode.addChild(playerNode);
    }

    public EventNode<InstanceEvent> getInstanceNode() {
        return instanceNode;
    }

    public EventNode<EntityInstanceEvent> getEntityInstanceNode() {
        return entityInstanceNode;
    }

    public EventNode<PlayerEvent> getPlayerNode() {
        return playerNode;
    }

    public void unregister() {
        parentNode.removeChild(childNode);
    }
}
