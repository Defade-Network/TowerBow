package net.defade.towerbow.game;

import net.defade.towerbow.fight.FightHandler;
import net.defade.towerbow.fight.InventoryManager;
import net.defade.towerbow.map.TowerBowMapGenerator;
import net.defade.towerbow.teams.TeamsManager;
import net.defade.towerbow.utils.GameEventNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import java.util.UUID;

public class GameInstance extends InstanceContainer {
    private static DynamicRegistry.Key<DimensionType> TOWERBOW_DIMENSION = MinecraftServer.getDimensionTypeRegistry()
            .register(
                    NamespaceID.from("defade:towerbow"),
                    DimensionType.builder()
                            .ambientLight(1.0F) // Fully lit
                            .build()
            );

    private static final AttributeModifier FREEZE_PLAYER_MODIFIER = new AttributeModifier(NamespaceID.from("defade:freeze_player"), -10000, AttributeOperation.ADD_VALUE);

    private final GameManager gameManager;
    private final GameEventNode gameEventNode = new GameEventNode(this, MinecraftServer.getGlobalEventHandler());

    private boolean acceptsPlayers = true;

    private final TeamsManager teamsManager = new TeamsManager(this);
    private final InventoryManager inventoryManager = new InventoryManager(this);
    private final FightHandler fightHandler = new FightHandler(this);

    public GameInstance(GameManager gameManager) {
        super(UUID.randomUUID(), TOWERBOW_DIMENSION);
        this.gameManager = gameManager;

        setGenerator(new TowerBowMapGenerator());

        new GameStartHandler(this);
    }

    public boolean acceptsPlayers() {
        return acceptsPlayers;
    }

    public void setAcceptsPlayers(boolean acceptsPlayers) {
        this.acceptsPlayers = acceptsPlayers;
    }

    public GameEventNode getEventNode() {
        return gameEventNode;
    }

    public TeamsManager getTeams() {
        return teamsManager;
    }

    /**
     * Starts the game.
     * This function will make sure that everything is ready to start the game.
     */
    public void startGame() {
        setAcceptsPlayers(false);

        teamsManager.giveAllPlayersTeams();

        // Give blindness to all players for 5 seconds and don't allow them to move
        Potion blindness = new Potion(PotionEffect.BLINDNESS, (byte) 1, 5 * 20);
        getPlayers().forEach(player -> {
            player.addEffect(blindness);
            player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(FREEZE_PLAYER_MODIFIER);
        });
        scheduler().scheduleTask(() -> {
            getPlayers().forEach(player -> player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(FREEZE_PLAYER_MODIFIER));
        }, TaskSchedule.seconds(5), TaskSchedule.immediate());

        inventoryManager.giveStartItems();
        fightHandler.enablePvp(true);
    }

    public void destroy() {
        gameManager.unregisterGame(this);
        getPlayers().forEach(player -> player.kick(Component.text("The instance is being destroyed.").color(NamedTextColor.RED)));
        MinecraftServer.getInstanceManager().unregisterInstance(this);
        gameEventNode.unregister();
    }
}
