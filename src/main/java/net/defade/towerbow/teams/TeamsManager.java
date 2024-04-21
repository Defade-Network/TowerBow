package net.defade.towerbow.teams;

import net.defade.towerbow.game.GameInstance;
import net.minestom.server.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TeamsManager {
    private static final int MAX_PLAYERS_PER_TEAM = 6;

    private final GameInstance gameInstance;
    private final Map<Player, Team> teams = new HashMap<>();

    public TeamsManager(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    public void setTeam(Player player, Team team) {
        if (getTeam(player) == team) return;

        if (isTeamFull(team)) {
            player.sendMessage("Cette équipe est pleine."); // TODO: colors
            return;
        }

        teams.put(player, team);
    }

    public Team getTeam(Player player) {
        return teams.get(player);
    }

    public boolean isTeamFull(Team team) {
        return teams.values().stream().filter(team::equals).count() >= MAX_PLAYERS_PER_TEAM;
    }

    public void givePlayerAvailableTeam(Player player) {
        if (getPlayers(Team.ORANGE).size() <= getPlayers(Team.PURPLE).size()) {
            setTeam(player, Team.ORANGE);
        } else {
            setTeam(player, Team.PURPLE);
        }
    }

    public Set<Player> getPlayers(Team team) {
        return teams.entrySet().stream()
                .filter(entry -> entry.getValue().equals(team))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public void giveAllPlayersTeams() {
        gameInstance.getPlayers().stream()
                .filter(player -> getTeam(player) == null)
                .forEach(this::givePlayerAvailableTeam);
    }
}
