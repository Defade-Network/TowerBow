package net.defade.towerbow.map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Pos;

public class MapConfig {
    private final Pos spawnPoint;

    private final Pos spawnStart;
    private final Pos spawnEnd;

    private final Pos firstTeamSpawnStart;
    private final Pos firstTeamSpawnEnd;

    private final Pos secondTeamSpawnStart;
    private final Pos secondTeamSpawnEnd;

    public MapConfig(String config) {
        JsonObject configObject = new Gson().fromJson(config, JsonObject.class);

        JsonObject spawnPointObject = configObject.getAsJsonObject("waiting_lobby");
        this.spawnPoint = readPos(spawnPointObject.getAsJsonArray("spawnpoint"));
        this.spawnStart = readPos(spawnPointObject.getAsJsonArray("start_pos"));
        this.spawnEnd = readPos(spawnPointObject.getAsJsonArray("end_pos"));

        JsonObject teamsSpawnObject = configObject.getAsJsonObject("teams_spawns");

        JsonObject firstTeamSpawnObject = teamsSpawnObject.getAsJsonObject("first_team");
        this.firstTeamSpawnStart = readPos(firstTeamSpawnObject.getAsJsonArray("start_pos"));
        this.firstTeamSpawnEnd = readPos(firstTeamSpawnObject.getAsJsonArray("end_pos"));

        JsonObject secondTeamSpawnObject = teamsSpawnObject.getAsJsonObject("second_team");
        this.secondTeamSpawnStart = readPos(secondTeamSpawnObject.getAsJsonArray("start_pos"));
        this.secondTeamSpawnEnd = readPos(secondTeamSpawnObject.getAsJsonArray("end_pos"));
    }

    public Pos getSpawnPoint() {
        return spawnPoint;
    }

    public Pos getSpawnStart() {
        return spawnStart;
    }

    public Pos getSpawnEnd() {
        return spawnEnd;
    }

    public Pos getFirstTeamSpawnStart() {
        return firstTeamSpawnStart;
    }

    public Pos getFirstTeamSpawnEnd() {
        return firstTeamSpawnEnd;
    }

    public Pos getSecondTeamSpawnStart() {
        return secondTeamSpawnStart;
    }

    public Pos getSecondTeamSpawnEnd() {
        return secondTeamSpawnEnd;
    }

    private static Pos readPos(JsonArray posArray) {
        return new Pos(posArray.get(0).getAsDouble(), posArray.get(1).getAsDouble(), posArray.get(2).getAsDouble());
    }
}
