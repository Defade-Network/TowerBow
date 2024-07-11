package net.defade.towerbow;

import io.github.togar2.pvp.PvpExtension;
import net.defade.towerbow.commands.BonusCommand;
import net.defade.towerbow.commands.KillCommand;
import net.defade.towerbow.commands.ReviveCommand;
import net.defade.towerbow.commands.StartCommand;
import net.defade.towerbow.game.GameManager;
import net.defade.towerbow.utils.TowerBowPlayer;
import net.minestom.server.MinecraftServer;

public class Main {
    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();

        PvpExtension.init();
        MinecraftServer.getConnectionManager().setPlayerProvider(TowerBowPlayer::new);

        minecraftServer.start();
        new GameManager();

        registerCommands();
    }

    private static void registerCommands() {
        MinecraftServer.getCommandManager().register(new StartCommand());
        MinecraftServer.getCommandManager().register(new BonusCommand());
        MinecraftServer.getCommandManager().register(new KillCommand());
        MinecraftServer.getCommandManager().register(new ReviveCommand());
    }
}