package net.defade.towerbow.commands;

import net.defade.minestom.player.Rank;
import net.defade.towerbow.fight.CombatMechanics;
import net.defade.towerbow.game.GameInstance;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class ReviveCommand extends Command {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public ReviveCommand() {
        super("revive");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(MM.deserialize("<red>Usage: /revive <player>"));
        });

        ArgumentEntity playerArgument = ArgumentType.Entity("player");

        playerArgument.setSuggestionCallback((sender, context, suggestion) -> {
            if (!(sender instanceof Player player)) {
                return;
            }

            for (Player players : ((GameInstance) player.getInstance()).getDeadPlayers()) {
                suggestion.addEntry(new SuggestionEntry(players.getUsername()));
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
            }

            if (CombatMechanics.isAlive(target)) {
                player.sendMessage(MM.deserialize("<red>Cannot revive a living player."));
                return;
            }

            target.setHealth(20);
            target.setGameMode(GameMode.SURVIVAL);

            player.sendMessage(MM.deserialize("<green>Revived <player>", Placeholder.component("player", target.getDisplayName())));
        }, playerArgument);
    }
}
