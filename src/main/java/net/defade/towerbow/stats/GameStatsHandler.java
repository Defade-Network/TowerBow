package net.defade.towerbow.stats;

import net.defade.minestom.database.MongoUtils;
import net.defade.towerbow.game.GameInstance;
import net.minestom.server.entity.Player;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameStatsHandler {
    private final GameInstance gameInstance;
    private long gameStartTime;

    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();

    public GameStatsHandler(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    public void initTracking() {
        gameStartTime = System.currentTimeMillis();
        gameInstance.getPlayingPlayers().forEach(player -> playerStats.put(player.getUuid(), new PlayerStats()));
    }

    public PlayerStats getPlayerStats(Player player) {
        return playerStats.get(player.getUuid());
    }

    public void saveStats() {
        Document gameStats = new Document("_id", gameInstance.getUuid().toString())
                .append("start_time", gameStartTime)
                .append("end_time", System.currentTimeMillis());
        playerStats.forEach((playerUUID, stats) -> gameStats.append(playerUUID.toString(), stats.getMongoDBDocument()));

        MongoUtils.runAsync(() -> MongoUtils.insertDocument("towerbow", gameStats));
    }
}
