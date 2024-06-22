package net.defade.towerbow;

import io.github.togar2.pvp.PvpExtension;
import net.defade.towerbow.debug.StartCommand;
import net.defade.towerbow.debug.KillDebugCommand;
import net.defade.towerbow.game.GameManager;
import net.minestom.server.MinecraftServer;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        propertiesDebug();
        MinecraftServer minecraftServer = MinecraftServer.init();
        debug();

        PvpExtension.init();
        new GameManager();

        minecraftServer.start();
    }

    private static void propertiesDebug() {
        System.setProperty("minestom.debug", "true");
        System.setProperty("server-port", "25570");
        System.setProperty("redis.host", "82.64.180.251");
        System.setProperty("redis.port", "6379");
        System.setProperty("redis.username", "default");
        System.setProperty("redis.password", "");
        System.setProperty("mongo.connection-string", "mongodb://defade:defade@82.64.180.251:27017/?authSource=defade");
        System.setProperty("mongo.database", "defade");
    }

    private static void debug() {
        MinecraftServer.getConnectionManager().setUuidProvider((playerConnection, s) -> {
            Random random = new Random(s.hashCode());
            long mostSigBits = random.nextLong() & 0x0000FFFFFFFFFFFFL | 0x0000000000004000L;
            long leastSigBits = random.nextLong() & 0x3FFFFFFFFFFFFFFFL | 0x8000000000000000L;
            return new java.util.UUID(mostSigBits, leastSigBits);
        });
        MinecraftServer.getCommandManager().register(new StartCommand());
        MinecraftServer.getCommandManager().register(new KillDebugCommand());
    }
}
