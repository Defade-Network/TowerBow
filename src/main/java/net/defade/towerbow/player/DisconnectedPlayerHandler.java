package net.defade.towerbow.player;

import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
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
                DisconnectedPlayer disconnectedPlayer = new DisconnectedPlayer(event.getPlayer());
                disconnectedPlayers.put(event.getPlayer().getUuid(), disconnectedPlayer);

                // Remove the player from the team
                event.getPlayer().getTeam().removeMember(event.getPlayer().getUsername());
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
                CombatMechanics.revivePlayer(player, CombatMechanics.getRemainingLives(player));
            });
    }
}
