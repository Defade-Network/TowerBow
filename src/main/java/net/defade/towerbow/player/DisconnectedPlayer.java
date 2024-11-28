package net.defade.towerbow.player;

import net.defade.towerbow.teams.TowerbowTeam;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

public class DisconnectedPlayer {
    private final TowerbowTeam team;
    private final CompoundBinaryTag tags;
    private final ItemStack[] inventory;

    public DisconnectedPlayer(Player player) {
        this.team = (TowerbowTeam) player.getTeam();
        this.tags = player.tagHandler().asCompound();
        this.inventory = player.getInventory().getItemStacks();
    }

    public void apply(Player player) {
        player.setTeam(team);
        player.tagHandler().updateContent(tags);

        for (int index = 0; index < inventory.length; index++) {
            player.getInventory().setItemStack(index, inventory[index]);
        }
    }
}
