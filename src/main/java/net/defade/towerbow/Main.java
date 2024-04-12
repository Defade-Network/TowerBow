package net.defade.towerbow;

import net.defade.towerbow.game.GameManager;
import net.minestom.server.MinecraftServer;

public class Main {
    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();

        new GameManager();

        minecraftServer.start();
    }
}