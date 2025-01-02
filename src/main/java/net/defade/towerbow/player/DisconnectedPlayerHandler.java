package net.defade.towerbow.player;

import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.scoreboard.Team;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisconnectedPlayerHandler {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Map<UUID, DisconnectedPlayer> disconnectedPlayers = new HashMap<>();

    public DisconnectedPlayerHandler(GameInstance gameInstance) {
        init(gameInstance);
    }

    public void init(GameInstance gameInstance) {
        gameInstance.getEventNode().getInstanceNode()
            .addListener(PlayerDisconnectEvent.class, event -> {
                Player player = event.getPlayer();
                DisconnectedPlayer disconnectedPlayer = new DisconnectedPlayer(player);
                disconnectedPlayers.put(player.getUuid(), disconnectedPlayer);

                Team team = player.getTeam();
                if (team != null) {
                    team.removeMember(player.getUsername());

                    // Check if the team is empty
                    if (team.getMembers().isEmpty()) {
                        // The team lost (all members died/quit), end the game
                        Team opposingTeam = gameInstance.getTeams().firstTeam() == team
                            ? gameInstance.getTeams().secondTeam()
                            : gameInstance.getTeams().firstTeam();
                        gameInstance.finishGame(opposingTeam, team);
                    }
                }

                gameInstance.sendMessage(MM.deserialize(
                    "<color:#aa0000>❌ " + player.getUsername() + "</color> <red>a quitté la partie.</red>"
                ));
            })
            .addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
                Player player = playerSpawnEvent.getPlayer();
                DisconnectedPlayer disconnectedPlayer = disconnectedPlayers.get(playerSpawnEvent.getPlayer().getUuid());
                if (disconnectedPlayer == null) {
                    player.kick(MM.deserialize("<red>An error occurred while trying to spawn you back into the game."));
                    return;
                }

                disconnectedPlayer.apply(player);
                gameInstance.getGamePlayHandler().getScoreboardManager().initScoreboardForPlayer(player);
                CombatMechanics.revivePlayer(player);
                player.setGlowing(true);

                gameInstance.sendMessage(MM.deserialize(
                    "<gold>🏹 " + player.getUsername() + "</gold><color:#15db1f> est revenu.</color>"
                ));
            });
    }
}
