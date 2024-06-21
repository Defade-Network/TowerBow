package net.defade.towerbow.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeleportDebugCommand extends Command {
    public TeleportDebugCommand() {
        super("teleport");

        ArgumentDouble xArg = ArgumentType.Double("x");
        ArgumentDouble yArg = ArgumentType.Double("y");
        ArgumentDouble zArg = ArgumentType.Double("z");
        ArgumentFloat yawArg = ArgumentType.Float("yaw");
        ArgumentFloat pitchArg = ArgumentType.Float("pitch");

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can execute this command!").color(NamedTextColor.RED));
                return;
            }

            double x = context.get("x");
            double y = context.get("y");
            double z = context.get("z");
            float yaw = context.get("yaw");
            float pitch = context.get("pitch");

            player.teleport(new Pos(x, y, z).withView(yaw, pitch));
        }, xArg, yArg, zArg, yawArg, pitchArg);
    }
}
