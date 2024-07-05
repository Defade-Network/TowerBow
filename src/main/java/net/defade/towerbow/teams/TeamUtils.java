package net.defade.towerbow.teams;

import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamBuilder;
import net.minestom.server.scoreboard.TeamManager;
import java.util.Set;
import java.util.function.Function;

public class TeamUtils {
    private static final int MAX_PLAYERS_PER_TEAM = 6;

    private static final Function<TeamManager, GameTeams> RANDOM_TEAMS_SUPPLIER = teamManager -> {
        switch ((int) (Math.random() * 3)) {
            case 0 -> {
                Team firstTeam = new TeamBuilder("Red", teamManager)
                        .teamDisplayName(Component.text("Red").color(TextColor.color(0xe74c3c)))
                        .teamColor(NamedTextColor.RED)
                        .collisionRule(TeamsPacket.CollisionRule.NEVER)
                        .build();
                Team secondTeam = new TeamBuilder("Blue", teamManager)
                        .teamDisplayName(Component.text("Blue").color(TextColor.color(0x3498db)))
                        .teamColor(NamedTextColor.BLUE)
                        .collisionRule(TeamsPacket.CollisionRule.NEVER)
                        .build();

                return new GameTeams(firstTeam, secondTeam);
            }
            case 1 -> {
                Team firstTeam = new TeamBuilder("Cyan", teamManager)
                        .teamDisplayName(Component.text("Cyan").color(TextColor.color(0x1abc9c)))
                        .teamColor(NamedTextColor.AQUA)
                        .collisionRule(TeamsPacket.CollisionRule.NEVER)
                        .build();
                Team secondTeam = new TeamBuilder("Yellow", teamManager)
                        .teamDisplayName(Component.text("Yellow").color(TextColor.color(0xf1c40f)))
                        .teamColor(NamedTextColor.YELLOW)
                        .collisionRule(TeamsPacket.CollisionRule.NEVER)
                        .build();

                return new GameTeams(firstTeam, secondTeam);
            }
            case 2 -> {
                Team firstTeam = new TeamBuilder("Orange", teamManager)
                        .teamDisplayName(Component.text("Orange").color(TextColor.color(0xfc7f11)))
                        .teamColor(NamedTextColor.GOLD)
                        .collisionRule(TeamsPacket.CollisionRule.NEVER)
                        .build();
                Team secondTeam = new TeamBuilder("Purple", teamManager)
                        .teamDisplayName(Component.text("Purple").color(TextColor.color(0x9b59b6)))
                        .teamColor(NamedTextColor.LIGHT_PURPLE)
                        .collisionRule(TeamsPacket.CollisionRule.NEVER)
                        .build();

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
        TeamManager teamManager = new TeamManager();
        registerTeamSending(gameInstance, teamManager);

        return RANDOM_TEAMS_SUPPLIER.apply(teamManager);
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

    private static void registerTeamSending(GameInstance gameInstance, TeamManager teamManager) {
        gameInstance.getEventNode().getPlayerNode()
                .addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
                    for (Team team : teamManager.getTeams()) {
                        playerSpawnEvent.getPlayer().sendPacket(team.createTeamsCreationPacket());
                    }
                });
    }
}
