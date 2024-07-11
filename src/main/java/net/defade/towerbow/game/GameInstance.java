package net.defade.towerbow.game;

import net.defade.minestom.amethyst.AmethystLoader;
import net.defade.minestom.amethyst.AmethystSource;
import net.defade.towerbow.bonus.BonusBlockManager;
import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.fight.InventoryManager;
import net.defade.towerbow.map.WorldHandler;
import net.defade.towerbow.teams.GameTeams;
import net.defade.towerbow.teams.TeamUtils;
import net.defade.towerbow.utils.GameEventNode;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GameInstance extends InstanceContainer {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final DynamicRegistry.Key<DimensionType> TOWERBOW_DIMENSION = MinecraftServer.getDimensionTypeRegistry()
            .register(
                    NamespaceID.from("defade:towerbow"),
                    DimensionType.builder()
                            .ambientLight(1.0F) // Fully lit
                            .build()
            );
    private static final WorldBorder INITIAL_WORLD_BORDER = new WorldBorder(100, 50, 50, 0, 0); // World border at the start of the game

    private final GameManager gameManager;
    private final GameEventNode gameEventNode = new GameEventNode(this, MinecraftServer.getGlobalEventHandler());

    private boolean acceptsPlayers = true;

    private final WorldHandler worldHandler = new WorldHandler(this);
    private final GameTeams gameTeams = TeamUtils.getRandomTeams(this);
    private final InventoryManager inventoryManager = new InventoryManager(this);

    private final GameStartHandler gameStartHandler = new GameStartHandler(this);
    private final GamePlayHandler gamePlayHandler = new GamePlayHandler(this);

    public GameInstance(GameManager gameManager, AmethystSource amethystSource) {
        super(UUID.randomUUID(), TOWERBOW_DIMENSION);
        this.gameManager = gameManager;

        AmethystLoader amethystLoader = new AmethystLoader(this, amethystSource);
        setChunkLoader(amethystLoader);

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

    public WorldHandler getWorldHandler() {
        return worldHandler;
    }

    public GameTeams getTeams() {
        return gameTeams;
    }

    public Set<Player> getAlivePlayers() {
        return getPlayers().stream().filter(CombatMechanics::isAlive).collect(Collectors.toSet());
    }

    public Set<Player> getDeadPlayers() {
        return getPlayers().stream().filter(CombatMechanics::isDead).collect(Collectors.toSet());
    }

    /**
     * Starts the game.
     * This function will make sure that everything is ready to start the game.
     */
    public void startGame() {
        setAcceptsPlayers(false);

        TeamUtils.giveAllPlayersTeams(gameTeams, getPlayers());
        inventoryManager.giveStartItems();

        gameStartHandler.stop();
        worldHandler.start();
        gamePlayHandler.start();

        createMap(); // Clear the lobby and create the floor and teleport players
    }

    public void finishGame(Team winningTeam, Team loosingTeam) {
        getPlayers().forEach(player -> {
            if (player.getTeam() == winningTeam) { // Player won
                player.sendMessage(MM.deserialize(
                        "<st><dark_gray>                                   </dark_gray></st>" +
                                "\n<gold>\uD83C\uDFF9 <b>VICTOIRE</b> <dark_gray>-</dark_gray> <winners> \uD83C\uDFF9</gold>" +
                                "\n\n<gray>»</gray> <yellow><b>" + CombatMechanics.getDamageDealt(player) / 2 + "</b>❤ Infligés</yellow>" +
                                "\n<gray>»</gray> <yellow><b>" + CombatMechanics.getKills(player) + "</b> Kills</yellow>" +
                                "\n\n<gray>»</gray> <yellow><b>" + CombatMechanics.getLongshotCount(player) + "</b> Longshots</yellow> <gray>(50+ blocks)</gray>" +
                                "\n<gray>»</gray> <yellow><b>" + BonusBlockManager.getBonusBlockCount(player) + "</b> Block Bonus</yellow>" +
                                "\n<st><dark_gray>                                   </dark_gray></st>",
                        Placeholder.component("winners", winningTeam.getTeamDisplayName())

                ));

                player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(7500),Duration.ofMillis(1000)));
                player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<b><gradient:#FFA751:#FFD959>VICTOIRE!</gradient></b>"));

                player.playSound(Sound.sound().type(SoundEvent.UI_TOAST_CHALLENGE_COMPLETE).pitch(1F).volume(0.5F).build(), player.getPosition());
            } else { // Player lost
                player.sendMessage(MM.deserialize(
                        "<st><dark_gray>                                   </dark_gray></st>" +
                                "\n<red>\uD83C\uDFF9 <b>DÉFAITE</b> <dark_gray>-</dark_gray> <loosers> \uD83C\uDFF9</red>" +
                                "\n\n<gray>»</gray> <yellow><b>" + CombatMechanics.getDamageDealt(player) / 2 + "</b>❤ Infligés</yellow>" +
                                "\n<gray>»</gray> <yellow><b>" + CombatMechanics.getKills(player) + "</b> Kills</yellow>" +
                                "\n\n<gray>»</gray> <yellow><b>" + CombatMechanics.getLongshotCount(player) + "</b> Longshots</yellow> <gray>(50+ blocks)</gray>" +
                                "\n<gray>»</gray> <yellow><b>" + BonusBlockManager.getBonusBlockCount(player) + "</b> Block Bonus</yellow>" +
                                "\n<st><dark_gray>                                   </dark_gray></st>",
                        Placeholder.component("loosers", loosingTeam.getTeamDisplayName())

                ));

                player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(0),Duration.ofMillis(7500),Duration.ofMillis(1000)));
                player.sendTitlePart(TitlePart.TITLE, MM.deserialize("<b><gradient:#E01E1E:#FF6464>DÉFAITE!</gradient></b>"));

                player.playSound(Sound.sound().type(SoundEvent.BLOCK_BEACON_DEACTIVATE).pitch(1F).volume(0.5F).build(), player.getPosition());
            }
            player.sendTitlePart(TitlePart.SUBTITLE, MM.deserialize("<gray>Pour rejouer, cliquez sur le papier</gray>"));
            player.sendActionBar(MM.deserialize(""));
            //Set all players in "Spectator like" but not in spectator otherwise they won't have a hotbar
            player.teleport(player.getPosition().add(0,0.1,0));

            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlying(true);
            player.setFlying(true);
            player.setInvulnerable(true);

            player.getInventory().clear(); //TODO: remember, don't give the replay paper before here
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

    private void createMap() {
        AbsoluteBlockBatch mapBatch = new AbsoluteBlockBatch();
        for (int x = 22; x <= 86; x++) {
            for (int y = 74; y <= 146; y++) {
                for (int z = 15; z <= 69; z++) {
                    mapBatch.setBlock(x, y, z, Block.AIR);
                }
            }
        }

        int mapSize = switch (getPlayers().size()) {
            case 4, 5 -> 60;
            case 6, 7 -> 70;
            case 8, 9 -> 80;
            case 10, 11 -> 90;
            case 12 -> 100;
            default -> 50;
        };

        for (int x = 0; x < mapSize; x++) {
            for (int z = 0; z < mapSize; z++) {
                mapBatch.setBlock(x, 0, z, Block.BLUE_STAINED_GLASS);
            }
        }

        mapBatch.apply(this, () -> {
            teleportPlayersToGame(mapSize);
            // We change the world border after teleporting the players, since they might get stuck inside
            setWorldBorder(INITIAL_WORLD_BORDER.withCenter(mapSize / 2D, mapSize / 2D).withDiameter(mapSize));
        });
    }

    private void teleportPlayersToGame(int mapSize) {
        Pos firstTeamEndPos = new Pos(mapSize, 0, mapSize)
                .sub(10)
                .withY(1);
        Pos firstTeamStartPos = firstTeamEndPos.sub(5).withY(1);

        Pos secondTeamStartPos = new Pos(0, 0, 0)
                .add(10)
                .withY(1);
        Pos secondTeamEndPos = secondTeamStartPos.add(5).withY(1);

        spreadPlayersAcrossPos(gameTeams.firstTeam().getPlayers(), firstTeamStartPos, firstTeamEndPos);
        spreadPlayersAcrossPos(gameTeams.secondTeam().getPlayers(), secondTeamStartPos, secondTeamEndPos);
    }

    private static void spreadPlayersAcrossPos(Collection<Player> players, Pos firstPos, Pos secondPos) {
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
