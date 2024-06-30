package net.defade.towerbow.commands;

import net.defade.minestom.player.Rank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class KillCommand extends Command {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public KillCommand() {
        super("kill");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(MM.deserialize("<red>Usage: /kill <player>"));
        });

        ArgumentEntity playerArgument = ArgumentType.Entity("player");

        playerArgument.setSuggestionCallback((sender, context, suggestion) -> {
            if (!(sender instanceof Player player)) {
                return;
            }

            for (Player players : player.getInstance().getPlayers()) {
                if (players.getGameMode() != GameMode.SPECTATOR) suggestion.addEntry(new SuggestionEntry(players.getUsername()));
            }
        });

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MM.deserialize("<red>Only players can execute this command."));
                return;
            }

            if (player.getRank() != Rank.ADMIN) {
                player.sendMessage(MM.deserialize("<red>You do not have permission to execute this command."));
                return;
            }

            Player target = context.get(playerArgument).findFirstPlayer(player);

            if (target == null) {
                player.sendMessage(MM.deserialize("<red>Unknown player."));
                return;
            } else if (target.getGameMode() == GameMode.SPECTATOR) {
                player.sendMessage(MM.deserialize("<red>Cannot kill a dead player."));
                return;
            }

            target.setHealth(0);
        }, playerArgument);
    }
}
