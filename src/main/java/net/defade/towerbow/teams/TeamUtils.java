package net.defade.towerbow.teams;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.game.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class TeamUtils {
    private static final int METADATA_OFFSET = 0;
    private static final byte HAS_GLOWING_EFFECT_BIT = 0x40;

    public static final int MAX_PLAYERS_PER_TEAM = GameManager.MAX_PLAYERS / 2;

    private static final Supplier<GameTeams> RANDOM_TEAMS_SUPPLIER = () -> {
        switch ((int) (Math.random() * 3)) {
            case 0 -> {
                TowerbowTeam firstTeam = new TowerbowTeam("Red");
                firstTeam.setTeamDisplayName(Component.text("Red").color(TextColor.color(0xe74c3c)));
                firstTeam.setTeamColor(NamedTextColor.RED);
                firstTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);

                TowerbowTeam secondTeam = new TowerbowTeam("Blue");
                secondTeam.setTeamDisplayName(Component.text("Blue").color(TextColor.color(0x3498db)));
                secondTeam.setTeamColor(NamedTextColor.BLUE);
                secondTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);

                return new GameTeams(firstTeam, secondTeam);
            }
            case 1 -> {
                TowerbowTeam firstTeam = new TowerbowTeam("Cyan");
                firstTeam.setTeamDisplayName(Component.text("Cyan").color(TextColor.color(0x1abc9c)));
                firstTeam.setTeamColor(NamedTextColor.AQUA);
                firstTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);

                TowerbowTeam secondTeam = new TowerbowTeam("Yellow");
                secondTeam.setTeamDisplayName(Component.text("Yellow").color(TextColor.color(0xf1c40f)));
                secondTeam.setTeamColor(NamedTextColor.YELLOW);
                secondTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);

                return new GameTeams(firstTeam, secondTeam);
            }
            case 2 -> {
                TowerbowTeam firstTeam = new TowerbowTeam("Orange");
                firstTeam.setTeamDisplayName(Component.text("Orange").color(TextColor.color(0xfc7f11)));
                firstTeam.setTeamColor(NamedTextColor.GOLD);
                firstTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);

                TowerbowTeam secondTeam = new TowerbowTeam("Purple");
                secondTeam.setTeamDisplayName(Component.text("Purple").color(TextColor.color(0x9b59b6)));
                secondTeam.setTeamColor(NamedTextColor.LIGHT_PURPLE);
                secondTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);

                return new GameTeams(firstTeam, secondTeam);
            }
        }

        return null;
    };

    public static Material getPlayerHelmetForTeam(Team team) {
        return switch (team.getTeamName()) {
            case "Red" -> Material.RED_STAINED_GLASS;
            case "Blue" -> Material.BLUE_STAINED_GLASS;
            case "Cyan" -> Material.CYAN_STAINED_GLASS;
            case "Yellow" -> Material.YELLOW_STAINED_GLASS;
            case "Orange" -> Material.ORANGE_STAINED_GLASS;
            case "Purple" -> Material.PURPLE_STAINED_GLASS;
            default -> Material.WHITE_STAINED_GLASS;
        };
    }

    public static GameTeams getRandomTeams(GameInstance gameInstance) {
        GameTeams gameTeams = RANDOM_TEAMS_SUPPLIER.get();
        registerTeamSending(gameInstance, gameTeams);

        return gameTeams;
    }

    public static boolean isTeamFull(Team team) {
        return team.getMembers().size() >= MAX_PLAYERS_PER_TEAM;
    }

    public static void givePlayerAvailableTeam(GameTeams gameTeams, Player player) {
        if (gameTeams.firstTeam().getMembers().size() <= gameTeams.secondTeam().getMembers().size()) {
            player.setTeam(gameTeams.firstTeam());
        } else {
            player.setTeam(gameTeams.secondTeam());
        }
    }

    public static void giveAllPlayersTeams(GameTeams gameTeams, Set<Player> players) {
        players.stream()
                .filter(player -> player.getTeam() == null)
                .forEach(player -> givePlayerAvailableTeam(gameTeams, player));
    }

    public static void registerTeamGlowing(GameInstance gameInstance) {
        gameInstance.getEventNode().getPlayerNode().addListener(PlayerPacketOutEvent.class, playerPacketOutEvent -> {
            if (playerPacketOutEvent.getPacket() instanceof EntityMetaDataPacket(
                int entityId, java.util.Map<Integer, Metadata.Entry<?>> entries
            )) {
                Player player = playerPacketOutEvent.getPlayer();
                Entity target = gameInstance.getEntityTracker().getEntityById(entityId);
                if (!(target instanceof Player targetPlayer)) return;

                Team playerTeam = player.getTeam();
                Team targetTeam = targetPlayer.getTeam();
                if (playerTeam == targetTeam) return;

                // Check if the glowing bit is set, if it is, we cancel the event and resend the packet with the glowing bit removed
                // If the glowing bit is not set, we let the packet go through
                Metadata.Entry<?> entry = entries.get(METADATA_OFFSET);
                if (entry != null) {
                    byte value = (byte) entry.value();
                    if ((value & HAS_GLOWING_EFFECT_BIT) != 0) {
                        playerPacketOutEvent.setCancelled(true);
                        // Modify the packet to remove the glowing effect
                        Map<Integer, Metadata.Entry<?>> newEntries = Map.of(METADATA_OFFSET, Metadata.Byte((byte) (value & ~HAS_GLOWING_EFFECT_BIT)));
                        player.sendPacket(new EntityMetaDataPacket(entityId, newEntries));
                    }
                }
            }
        });
    }

    private static void registerTeamSending(GameInstance gameInstance, GameTeams gameTeams) {
        gameInstance.getEventNode().getPlayerNode()
                .addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
                    playerSpawnEvent.getPlayer().sendPacket(gameTeams.firstTeam().createTeamsCreationPacket());
                    playerSpawnEvent.getPlayer().sendPacket(gameTeams.secondTeam().createTeamsCreationPacket());
                })
                .addListener(PlayerPacketOutEvent.class, playerPacketOutEvent -> {
                    if (!(playerPacketOutEvent.getPacket() instanceof TeamsPacket teamsPacket)) return;

                    Player player = playerPacketOutEvent.getPlayer();
                    String teamName = teamsPacket.teamName();
                    if (!gameInstance.getTeams().firstTeam().getTeamName().equals(teamName) &&
                        !gameInstance.getTeams().secondTeam().getTeamName().equals(teamName)) return; // This is a scoreboard packet or something else, not the players team

                    // Remove all the entities in the entity list if they are not in the same instance as the player
                    if (teamsPacket.action() instanceof TeamsPacket.CreateTeamAction(
                        Component displayName, byte friendlyFlags, TeamsPacket.NameTagVisibility nameTagVisibility,
                        TeamsPacket.CollisionRule collisionRule, NamedTextColor teamColor, Component teamPrefix,
                        Component teamSuffix, Collection<String> entities)) {

                        if (entitiesNeedsToBeRemoved(entities, player)) {
                            playerPacketOutEvent.setCancelled(true);

                            TeamsPacket.CreateTeamAction newCreateTeamAction = new TeamsPacket.CreateTeamAction(
                                displayName,
                                friendlyFlags,
                                nameTagVisibility,
                                collisionRule,
                                teamColor,
                                teamPrefix,
                                teamSuffix,
                                removeEntitiesFromSeparateInstances(entities, player)
                            );

                            TeamsPacket newTeamsPacket = new TeamsPacket(teamsPacket.teamName(), newCreateTeamAction);
                            player.sendPacket(newTeamsPacket);
                        }
                    } else if (teamsPacket.action() instanceof TeamsPacket.AddEntitiesToTeamAction(Collection<String> entities)) {
                        if (entitiesNeedsToBeRemoved(entities, player)) {
                            playerPacketOutEvent.setCancelled(true);

                            TeamsPacket.AddEntitiesToTeamAction newAddPlayerAction = new TeamsPacket.AddEntitiesToTeamAction(
                                removeEntitiesFromSeparateInstances(entities, player)
                            );

                            TeamsPacket newTeamsPacket = new TeamsPacket(teamsPacket.teamName(), newAddPlayerAction);
                            player.sendPacket(newTeamsPacket);
                        }
                    } else if (teamsPacket.action() instanceof TeamsPacket.RemoveEntitiesToTeamAction(Collection<String> entities)) {
                        if (entitiesNeedsToBeRemoved(entities, player)) {
                            playerPacketOutEvent.setCancelled(true);

                            TeamsPacket.RemoveEntitiesToTeamAction newRemovePlayerAction = new TeamsPacket.RemoveEntitiesToTeamAction(
                                removeEntitiesFromSeparateInstances(entities, player)
                            );

                            TeamsPacket newTeamsPacket = new TeamsPacket(teamsPacket.teamName(), newRemovePlayerAction);
                            player.sendPacket(newTeamsPacket);
                        }
                    }
                });
    }

    private static boolean entitiesNeedsToBeRemoved(Collection<String> entities, Player player) {
        return entities.stream().anyMatch(entity -> {
            Entity target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(entity);
            return target == null || target.getInstance() != player.getInstance();
        });
    }

    private static Collection<String> removeEntitiesFromSeparateInstances(Collection<String> entities, Player player) {
        // entities is an immutable collection, so we need to create a new one
        List<String> newEntities = new ArrayList<>(entities);
        newEntities.removeIf(entity -> {
            Entity target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(entity);
            return target == null || target.getInstance() != player.getInstance();
        });

        return newEntities;
    }
}
