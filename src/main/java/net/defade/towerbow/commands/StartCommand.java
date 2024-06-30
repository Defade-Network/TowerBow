package net.defade.towerbow.commands;

import net.defade.minestom.player.Rank;
import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class StartCommand extends Command {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public StartCommand() {
        super("start");

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MM.deserialize("<red>Only players can execute this command."));
                return;
            }

            if (player.getRank() != Rank.ADMIN) {
                player.sendMessage(MM.deserialize("<red>You do not have permission to execute this command."));
                return;
            }

            player.sendMessage(MM.deserialize("<green>Starting the game..."));
            ((GameInstance) player.getInstance()).startGame();
        });
    }
}
