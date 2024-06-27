package net.defade.towerbow;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import io.github.togar2.pvp.PvpExtension;
import net.defade.minestom.amethyst.AmethystLoader;
import net.defade.towerbow.debug.HideDebugCommand;
import net.defade.towerbow.debug.StartCommand;
import net.defade.towerbow.debug.KillDebugCommand;
import net.defade.towerbow.game.GameManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.world.DimensionType;
import org.bson.BsonString;
import org.bson.Document;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        propertiesDebug();
        MinecraftServer minecraftServer = MinecraftServer.init();
        debug();

        PvpExtension.init();

        minecraftServer.start();
        new GameManager();

        //convertMap();
        //Thread.sleep(1000L);
        //uploadMap();
    }

    private static void convertMap() {
        InstanceContainer anvil = new InstanceContainer(UUID.randomUUID(), DimensionType.OVERWORLD);
        anvil.setChunkLoader(new AnvilLoader("/home/gustave/.local/share/PrismLauncher/instances/Defade Dev(2)/.minecraft/saves/world"));

        //load the map
        for (int x = 0; x <= 100; x++) {
            for (int z = 0; z <= 100; z++) {
                anvil.loadChunk(x / 16, z / 16).join();
            }
        }

        // Set loader to amethyst loader
        Path amethystPath = Path.of("/home/gustave/Documents/towerbow.amethyst");
        try {
            Files.deleteIfExists(amethystPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        AmethystLoader amethystLoader = new AmethystLoader(anvil, amethystPath);
        String config;

        // Use GSON lib to write the config
        JsonObject jsonObject = new JsonObject();

        JsonObject teamSpawns = new JsonObject();
        JsonObject team1Spawns = new JsonObject();
        JsonArray team1StartPos = new JsonArray();
        team1StartPos.add(12);
        team1StartPos.add(1);
        team1StartPos.add(12);
        team1Spawns.add("start_pos", team1StartPos);
        JsonArray team1EndPos = new JsonArray();
        team1EndPos.add(18);
        team1EndPos.add(1);
        team1EndPos.add(18);
        team1Spawns.add("end_pos", team1EndPos);
        JsonObject team2Spawns = new JsonObject();
        JsonArray team2StartPos = new JsonArray();
        team2StartPos.add(82);
        team2StartPos.add(1);
        team2StartPos.add(82);
        team2Spawns.add("start_pos", team2StartPos);
        JsonArray team2EndPos = new JsonArray();
        team2EndPos.add(88);
        team2EndPos.add(1);
        team2EndPos.add(88);
        team2Spawns.add("end_pos", team2EndPos);
        teamSpawns.add("first_team", team1Spawns);
        teamSpawns.add("second_team", team2Spawns);

        JsonObject waitingLobby = new JsonObject();
        JsonArray waitingLobbyStartPos = new JsonArray();
        waitingLobbyStartPos.add(22);
        waitingLobbyStartPos.add(74);
        waitingLobbyStartPos.add(15);
        waitingLobby.add("start_pos", waitingLobbyStartPos);
        JsonArray waitingLobbyEndPos = new JsonArray();
        waitingLobbyEndPos.add(86);
        waitingLobbyEndPos.add(146);
        waitingLobbyEndPos.add(69);
        waitingLobby.add("end_pos", waitingLobbyEndPos);
        JsonArray waitingLobbySpawnpoint = new JsonArray();
        waitingLobbySpawnpoint.add(55.5);
        waitingLobbySpawnpoint.add(100);
        waitingLobbySpawnpoint.add(52.5);
        waitingLobby.add("spawnpoint", waitingLobbySpawnpoint);

        jsonObject.add("teams_spawns", teamSpawns);
        jsonObject.add("waiting_lobby", waitingLobby);

        config = jsonObject.toString();
        amethystLoader.setWorldConfig(config.getBytes(StandardCharsets.UTF_8));

        anvil.setChunkLoader(amethystLoader);
        anvil.saveChunksToStorage().join();
    }

    private static void uploadMap() {
        Path amethystPath = Path.of("/home/gustave/Documents/towerbow.amethyst");
        String md5 = getMd5(amethystPath);
        // Upload map to gridfs
        GridFSBucket gridFSBucket = GridFSBuckets.create(MinecraftServer.getMongoDatabase(), "game-maps");
        // Delete the file before uploading
        if (gridFSBucket.find(new Document("_id", "towerbow-default-map")).first() != null) gridFSBucket.delete(new BsonString("towerbow-default-map"));
        try {
            gridFSBucket.uploadFromStream(new BsonString("towerbow-default-map"), "towerbow-default-map", Files.newInputStream(amethystPath),
                    new GridFSUploadOptions().metadata(new Document("md5", md5).append("game", "towerbow")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void propertiesDebug() {
        System.setProperty("minestom.debug", "true");
        System.setProperty("server-port", "25570");
        System.setProperty("redis.host", "82.64.180.251");
        System.setProperty("redis.port", "6379");
        System.setProperty("redis.username", "default");
        System.setProperty("redis.password", "defaderedis");
        System.setProperty("mongo.connection-string", "mongodb://defade:defade@192.168.1.107:27017/?authSource=defade");
        System.setProperty("mongo.database", "defade");
    }

    private static void debug() {
        MinecraftServer.getConnectionManager().setUuidProvider((playerConnection, s) -> {
            Random random = new Random(s.hashCode() + 1);
            long mostSigBits = random.nextLong() & 0x0000FFFFFFFFFFFFL | 0x0000000000004000L;
            long leastSigBits = random.nextLong() & 0x3FFFFFFFFFFFFFFFL | 0x8000000000000000L;
            return new java.util.UUID(mostSigBits, leastSigBits);
        });
        MinecraftServer.getCommandManager().register(new StartCommand());
        MinecraftServer.getCommandManager().register(new KillDebugCommand());
        MinecraftServer.getCommandManager().register(new HideDebugCommand());
    }

    private static String getMd5(Path path) {
        // Do this without using any external library

        String md5 = "";
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            byte[] md5Bytes = java.security.MessageDigest.getInstance("MD5").digest(fileBytes);
            md5 = new BigInteger(1, md5Bytes).toString(16);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return md5;
    }
}