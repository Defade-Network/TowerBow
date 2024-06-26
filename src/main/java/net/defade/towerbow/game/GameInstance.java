package net.defade.towerbow.game;

import net.defade.towerbow.fight.InventoryManager;
import net.defade.towerbow.map.WorldHandler;
import net.defade.towerbow.teams.Team;
import net.defade.towerbow.teams.TeamsManager;
import net.defade.towerbow.utils.GameEventNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import java.util.UUID;

public class GameInstance extends InstanceContainer {
    private static final DynamicRegistry.Key<DimensionType> TOWERBOW_DIMENSION = MinecraftServer.getDimensionTypeRegistry()
            .register(
                    NamespaceID.from("defade:towerbow"),
                    DimensionType.builder()
                            .ambientLight(1.0F) // Fully lit
                            .build()
            );

    public static final int MAP_SIZE = 100; // 100x100 blocks
    private static final WorldBorder INITIAL_WORLD_BORDER = new WorldBorder(MAP_SIZE, 0, 0, 0, 0); // World border at the start of the game

    private final GameManager gameManager;
    private final GameEventNode gameEventNode = new GameEventNode(this, MinecraftServer.getGlobalEventHandler());

    private boolean acceptsPlayers = true;

    private final TeamsManager teamsManager = new TeamsManager(this);
    private final InventoryManager inventoryManager = new InventoryManager(this);

    private final GameStartHandler gameStartHandler = new GameStartHandler(this);
    private final GamePlayHandler gamePlayHandler = new GamePlayHandler(this);

    public GameInstance(GameManager gameManager) {
        super(UUID.randomUUID(), TOWERBOW_DIMENSION);
        this.gameManager = gameManager;

        WorldHandler.register(this);
        setWorldBorder(INITIAL_WORLD_BORDER);
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

        getPlayers().forEach(player -> player.setEnableRespawnScreen(false)); // Disable respawn screen

        teamsManager.giveAllPlayersTeams();
        inventoryManager.giveStartItems(); // TODO: manage inventory with a state handler

        gameStartHandler.stop();
        gamePlayHandler.start();
    }

    public void finishGame(Team winningTeam) {
        sendMessage(Component.text("Partie finie! Victoire des " + winningTeam.name() + "!")); // TODO better message
        gamePlayHandler.stop();

        scheduler().scheduleTask(this::destroy, TaskSchedule.seconds(20), TaskSchedule.stop());
    }

    public void destroy() {
        gameManager.unregisterGame(this);
        getPlayers().forEach(player -> player.kick(Component.text("The instance is being destroyed.").color(NamedTextColor.RED)));
        MinecraftServer.getInstanceManager().unregisterInstance(this);
        gameEventNode.unregister();
    }
}
