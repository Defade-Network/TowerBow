package net.defade.towerbow.fight;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.utils.Items;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.utils.time.TimeUnit;

public class InventoryManager {
    private final GameInstance gameInstance;

    public InventoryManager(GameInstance gameInstance) {
        this.gameInstance = gameInstance;

        registerForbiddenItemMoves();
        registerGoldenAppleDrop();
    }

    public void giveStartItems() {
        gameInstance.getPlayingPlayers().forEach(this::giveItemsToPlayer);
    }

    public void giveItemsToPlayer(Player player) {
        player.getInventory().clear();

        player.getInventory().setHelmet(Items.HELMET.apply(player.getTeam()));
        player.getInventory().setChestplate(Items.CHESTPLATE.apply(player.getTeam()));
        player.getInventory().setLeggings(Items.LEGGINGS);
        player.getInventory().setBoots(Items.BOOTS);

        player.getInventory().setItemStack(0, Items.GOLDEN_PICKAXE);
        player.getInventory().setItemInOffHand(Items.COBBLESTONE);
        player.getInventory().setItemStack(1, Items.BOW);
        player.getInventory().setItemStack(8, Items.GOLDEN_APPLE);

        player.getInventory().setItemStack(9, Items.ARROW);
    }

    private void registerForbiddenItemMoves() {
        // Don't let the player move the armor
        gameInstance.getEventNode().getPlayerNode().addListener(InventoryPreClickEvent.class, inventoryClickEvent -> {
            if (inventoryClickEvent.getSlot() >= 41 && inventoryClickEvent.getSlot() <= 44) { // Armor slots are 41-44
                inventoryClickEvent.setCancelled(true);
            }
        });
    }

    private void registerGoldenAppleDrop() {
        // Only let the players drop the golden apple
        gameInstance.getEventNode().getPlayerNode().addListener(ItemDropEvent.class, itemDropEvent -> {
            if (itemDropEvent.getPlayer().getGameMode() == GameMode.SPECTATOR ||  !itemDropEvent.getItemStack().isSimilar(Items.GOLDEN_APPLE)) {
                itemDropEvent.setCancelled(true);
            } else {
                // Drop the item
                ItemEntity itemEntity = new ItemEntity(itemDropEvent.getItemStack());
                itemEntity.setPickupDelay(40, TimeUnit.SERVER_TICK);
                itemEntity.setInstance(itemDropEvent.getPlayer().getInstance(), itemDropEvent.getPlayer().getPosition().add(0, 1.5, 0));
                itemEntity.setVelocity(itemDropEvent.getPlayer().getPosition().direction().mul(6));
            }
        });

        // Register the golden apple pickup event
        gameInstance.getEventNode().getEntityInstanceNode().addListener(PickupItemEvent.class, pickupItemEvent -> {
            if (!(pickupItemEvent.getEntity() instanceof Player player)) return;

            if (pickupItemEvent.getItemEntity().getItemStack().isSimilar(Items.GOLDEN_APPLE)) {
                player.getInventory().addItemStack(Items.GOLDEN_APPLE);
            }
        });
    }
}
