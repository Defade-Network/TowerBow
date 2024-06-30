package net.defade.towerbow.teams;

import net.minestom.server.scoreboard.Team;

/**
 * Holds the two teams confronting each other in a game.
 * @param firstTeam The first team
 * @param secondTeam The second team
 */
public record GameTeams(Team firstTeam, Team secondTeam) { }
