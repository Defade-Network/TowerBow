package net.defade.towerbow.teams;

import net.defade.towerbow.fight.CombatMechanics;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class TowerbowTeam extends Team {
    /**
     * Default constructor to creates a team.
     *
     * @param teamName The registry name for the team
     */
    public TowerbowTeam(@NotNull String teamName) {
        super(teamName);
    }

    /**
     * @return the alive players in the team
     */
    @Override
    public @NotNull Collection<Player> getPlayers() {
        return super.getPlayers()
                .stream()
                .filter(CombatMechanics::isAlive)
                .toList();
    }

    /**
     * @return the alive members in the team
     */
    @Override
    public @NotNull Set<String> getMembers() {
        return super.getMembers()
                .stream()
                .filter(name -> getPlayers().stream().anyMatch(player -> player.getUsername().equals(name)))
                .collect(Collectors.toSet());
    }

    /**
     * @return all players in the team, including the dead ones
     */
    public @NotNull Collection<Player> getAllPlayers() {
        return super.getPlayers();
    }

    /**
     * @return all members in the team, including the dead ones
     */
    public @NotNull Set<String> getAllMembers() {
        return super.getMembers();
    }
}
