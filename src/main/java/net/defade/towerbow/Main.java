package net.defade.towerbow;

import io.github.togar2.pvp.PvpExtension;
import net.defade.towerbow.game.GameManager;
import net.minestom.server.MinecraftServer;

public class Main {
    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();

        PvpExtension.init();
        new GameManager();

        minecraftServer.start();
    }
}