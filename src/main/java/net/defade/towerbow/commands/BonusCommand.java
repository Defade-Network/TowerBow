package net.defade.towerbow.commands;

import net.defade.minestom.player.Rank;
import net.defade.towerbow.bonus.Bonus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import java.util.Arrays;

public class BonusCommand extends Command {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public BonusCommand() {
        super("bonus");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(MM.deserialize("<red>Usage: /bonus <bonus type>"));
        });

        ArgumentString bonusTypeArgument = ArgumentType.String("bonus-type");
        bonusTypeArgument.setSuggestionCallback((sender, context, suggestion) -> {
            for (Bonus bonusTypes : Bonus.values()) {
                suggestion.addEntry(new SuggestionEntry(bonusTypes.getName().replace(" ", "_").toLowerCase()));
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

            String bonusType = context.get(bonusTypeArgument);
            Bonus bonus = Arrays.stream(Bonus.values())
                    .filter(b -> b.getName().replace(" ", "_").equalsIgnoreCase(bonusType))
                    .findFirst()
                    .orElse(null);

            if (bonus == null) {
                player.sendMessage(MM.deserialize("<red>Unknown bonus type: " + bonusType));
                return;
            }

            bonus.getBonusBlock().onHit(player);
            player.sendMessage(MM.deserialize("<green>You received the bonus " + bonusType));
        }, bonusTypeArgument);
    }
}
