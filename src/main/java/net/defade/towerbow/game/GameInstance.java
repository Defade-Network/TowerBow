package net.defade.towerbow.game;

import net.defade.minestom.amethyst.AmethystLoader;
import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.fight.InventoryManager;
import net.defade.towerbow.map.MapConfig;
import net.defade.towerbow.map.WorldHandler;
import net.defade.towerbow.teams.Team;
import net.defade.towerbow.teams.TeamsManager;
import net.defade.towerbow.utils.GameEventNode;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GameInstance extends InstanceContainer {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final DynamicRegistry.Key<DimensionType> TOWERBOW_DIMENSION = MinecraftServer.getDimensionTypeRegistry()
            .register(
                    NamespaceID.from("defade:towerbow"),
                    DimensionType.builder()
                            .ambientLight(1.0F) // Fully lit
                            .build()
            );

    public static final int MAP_SIZE = 100; // 100x100 blocks
    private static final WorldBorder INITIAL_WORLD_BORDER = new WorldBorder(MAP_SIZE, 50, 50, 0, 0); // World border at the start of the game

    private final GameManager gameManager;
    private final MapConfig mapConfig;
    private final GameEventNode gameEventNode = new GameEventNode(this, MinecraftServer.getGlobalEventHandler());

    private boolean acceptsPlayers = true;

    private final TeamsManager teamsManager = new TeamsManager(this);
    private final InventoryManager inventoryManager = new InventoryManager(this);

    private final GameStartHandler gameStartHandler = new GameStartHandler(this);
    private final GamePlayHandler gamePlayHandler = new GamePlayHandler(this);

    public GameInstance(GameManager gameManager, Path mapPath) {
        super(UUID.randomUUID(), TOWERBOW_DIMENSION);
        this.gameManager = gameManager;

        AmethystLoader amethystLoader = new AmethystLoader(this, mapPath);
        this.mapConfig = new MapConfig(new String(amethystLoader.getWorldConfig()));
        setChunkLoader(amethystLoader);

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

    public MapConfig getMapConfig() {
        return mapConfig;
    }

    /**
     * Starts the game.
     * This function will make sure that everything is ready to start the game.
     */
    public void startGame() {
        setAcceptsPlayers(false);

        getPlayers().forEach(player -> player.setEnableRespawnScreen(false)); // Disable respawn screen

        // Clear the spawn area
        AbsoluteBlockBatch clearBatch = new AbsoluteBlockBatch();
        for (int x = mapConfig.getSpawnStart().blockX(); x <= mapConfig.getSpawnEnd().blockX(); x++) {
            for (int y = mapConfig.getSpawnStart().blockY(); y <= mapConfig.getSpawnEnd().blockY(); y++) {
                for (int z = mapConfig.getSpawnStart().blockZ(); z <= mapConfig.getSpawnEnd().blockZ(); z++) {
                    clearBatch.setBlock(x, y, z, Block.AIR);
                }
            }
        }
        clearBatch.apply(this, null);

        teamsManager.giveAllPlayersTeams();
        inventoryManager.giveStartItems(); // TODO: manage inventory with a state handler

        gameStartHandler.stop();
        gamePlayHandler.start();

        // Teleport players to their spawn points
        spreadPlayersAcrossPos(teamsManager.getPlayers(teamsManager.getGameTeams().firstTeam()), mapConfig.getFirstTeamSpawnStart(), mapConfig.getFirstTeamSpawnEnd());
        spreadPlayersAcrossPos(teamsManager.getPlayers(teamsManager.getGameTeams().secondTeam()), mapConfig.getSecondTeamSpawnStart(), mapConfig.getSecondTeamSpawnEnd());
    }

    public void finishGame(Team winningTeam, Team loosingTeam) {
        getPlayers().forEach(player -> {
            if (getTeams().getTeam(player) == winningTeam) { // Player won
                player.sendMessage(MM.deserialize(
                        "<st><dark_gray>                                   </dark_gray></st>" +
                                "\n<gold>\uD83C\uDFF9 <b>VICTOIRE</b> <dark_gray>-</dark_gray> <winners> \uD83C\uDFF9</gold>" +
                                "\n\n<gray>»</gray> <yellow><b>" + player.getTag(CombatMechanics.PLAYER_KILLS) + "</b> Kills</yellow>" +
                                "\n<gray>»</gray> <yellow>" + /* TODO: Block bonus count */ " Block Bonus</yellow>" +
                                "\n<st><dark_gray>                                   </dark_gray></st>",
                        Placeholder.component("winners", winningTeam.name().color(TextColor.color(winningTeam.color())))

                ));

                player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(7500),Duration.ofMillis(1000)));
                player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<gold><b>VICTOIRE!</b></gold>"));
                player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<gray>Pour rejouer, cliquez sur le papier</gray>"));

                player.playSound(Sound.sound().type(SoundEvent.UI_TOAST_CHALLENGE_COMPLETE).pitch(1F).volume(0.5F).build(), player.getPosition());
            } else { // Player lost
                player.sendMessage(MM.deserialize(
                        "<st><dark_gray>                                   </dark_gray></st>" +
                                "\n<red>\uD83C\uDFF9 <b>DÉFAITE</b> <dark_gray>-</dark_gray> <loosers> \uD83C\uDFF9</red>" +
                                "\n\n<gray>»</gray> <yellow><b>" + player.getTag(CombatMechanics.PLAYER_KILLS) + "</b> Kills</yellow>" +
                                "\n<gray>»</gray> <yellow>" + /* TODO: Block bonus count */ " Block Bonus</yellow>" +
                                "\n<st><dark_gray>                                   </dark_gray></st>",
                        Placeholder.component("loosers", loosingTeam.name().color(TextColor.color(loosingTeam.color())))

                ));

                player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(7500),Duration.ofMillis(1000)));
                player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<red><b>DÉFAITE!</b></red>"));
                player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<gray>Pour rejouer, cliquez sur le papier</gray>"));

                player.playSound(Sound.sound().type(SoundEvent.BLOCK_BEACON_DEACTIVATE).pitch(1F).volume(0.5F).build(), player.getPosition());
            }
            player.setGameMode(GameMode.SPECTATOR);
        });
        gamePlayHandler.stop();

        scheduler().scheduleTask(this::destroy, TaskSchedule.seconds(20), TaskSchedule.stop());
    }

    public void destroy() {
        gameManager.unregisterGame(this);
        getPlayers().forEach(player -> player.kick(Component.text("The instance is being destroyed.").color(NamedTextColor.RED)));
        MinecraftServer.getInstanceManager().unregisterInstance(this);
        gameEventNode.unregister();
    }

    private static void spreadPlayersAcrossPos(Set<Player> players, Pos firstPos, Pos secondPos) {
        Iterator<Player> playerIterator = players.iterator();

        for (int x = firstPos.blockX(); x <= secondPos.blockX(); x += 2) {
            for (int z = firstPos.blockZ(); z <= secondPos.blockZ(); z += 2) {
                if (!playerIterator.hasNext()) {
                    return;
                }

                Player player = playerIterator.next();
                player.teleport(new Pos(x, firstPos.blockY(), z).add(0.5, 0, 0.5)); // Spawn in center of block
            }
        }
    }
}
