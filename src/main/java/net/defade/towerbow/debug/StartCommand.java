package net.defade.towerbow.debug;

import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class StartCommand extends Command {
    public StartCommand() {
        super("start");

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Only players can execute this command!").color(NamedTextColor.RED));
                return;
            }

            Player player = (Player) sender;
            player.sendMessage(Component.text("Forcefully starting the game...").color(NamedTextColor.GREEN));

            ((GameInstance) player.getInstance()).startGame();
        });
    }
}
