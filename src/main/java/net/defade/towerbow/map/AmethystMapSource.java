package net.defade.towerbow.map;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import net.minestom.server.MinecraftServer;
import org.bson.Document;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

public class AmethystMapSource {
    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir") + File.separator + "defade");

    private final GridFSBucket mapBucket;

    public AmethystMapSource() {
        this.mapBucket = GridFSBuckets.create(MinecraftServer.getMongoDatabase(), "game-maps");
    }

    public Path getRandomMap() {
        Document mapDocument = MinecraftServer.getMongoDatabase().getCollection("game-maps.files")
                .aggregate(
                        Arrays.asList(
                                Aggregates.match(Filters.eq("metadata.game", "towerbow")),
                                Aggregates.sample(1)
                        )
                ).first();

        if (mapDocument == null) {
            return null;
        }

        String fileName = mapDocument.getString("filename");
        Path mapFile = TEMP_DIR.resolve(fileName);

        try {
            if (!Files.exists(TEMP_DIR)) {
                Files.createDirectory(TEMP_DIR);
            } else if (Files.exists(mapFile)) {
                if (getMd5(mapFile).equals(mapDocument.getString("md5"))) {
                    return mapFile;
                }
            }

            Files.deleteIfExists(mapFile); // In case the file already exists but is different
            Files.createFile(mapFile);

            try (OutputStream outputStream = Files.newOutputStream(mapFile)) {
                mapBucket.downloadToStream(fileName, outputStream);
            }

            return mapFile;
        } catch (Exception exception) {
            MinecraftServer.getExceptionManager().handleException(exception);
            return null;
        }
    }

    private static String getMd5(Path path) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(Files.readAllBytes(path));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | IOException ignored) { }

        return "";
    }
}
