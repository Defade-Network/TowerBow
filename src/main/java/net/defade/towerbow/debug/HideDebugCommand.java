package net.defade.towerbow.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;

public class HideDebugCommand extends Command {
    public HideDebugCommand() {
        super("hide");

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can execute this command!").color(NamedTextColor.RED));
                return;
            }

            player.setGameMode(GameMode.SPECTATOR);
            player.setInvisible(true); // Hide the player
            // Update visible players
        });
    }
}
