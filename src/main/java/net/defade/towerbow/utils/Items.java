package net.defade.towerbow.utils;

import net.defade.towerbow.teams.Team;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.DyedItemColor;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.component.Unbreakable;
import net.minestom.server.item.enchant.Enchantment;
import java.util.Map;
import java.util.function.Function;

public class Items {
    public static final Function<Team, ItemStack> HELMET = team -> ItemStack.of(team.playerHelmet());

    public static final Function<Team, ItemStack> CHESTPLATE = team -> {
        DyedItemColor teamColor = new DyedItemColor(team.color());

        return ItemStack.of(Material.LEATHER_CHESTPLATE)
                .with(ItemComponent.DYED_COLOR, teamColor)
                .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                        Map.of(
                                Enchantment.PROTECTION, 2,
                                Enchantment.FEATHER_FALLING, 3
                        )
                ));
    };

    public static final ItemStack LEGGINGS = ItemStack.of(Material.IRON_LEGGINGS)
            .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                    Map.of(
                            Enchantment.PROTECTION, 2,
                            Enchantment.FEATHER_FALLING, 3
                    )
            ));

    public static final ItemStack BOOTS = ItemStack.of(Material.IRON_BOOTS)
            .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                    Map.of(
                            Enchantment.PROTECTION, 2,
                            Enchantment.FEATHER_FALLING, 3
                    )
            ));

    public static final ItemStack GOLDEN_APPLE = ItemStack.of(Material.GOLDEN_APPLE);

    public static final ItemStack GOLDEN_PICKAXE = ItemStack.of(Material.GOLDEN_PICKAXE)
            .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                    Map.of(
                            Enchantment.EFFICIENCY, 2,
                            Enchantment.SHARPNESS, 1
                    )
            ));

    public static final ItemStack BOW = ItemStack.of(Material.BOW)
            .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                    Map.of(
                            Enchantment.INFINITY, 1,
                            Enchantment.PUNCH, 1,
                            Enchantment.POWER, 2
                    )
            ))
            .with(ItemComponent.UNBREAKABLE, new Unbreakable(false));

    public static final ItemStack ARROW = ItemStack.of(Material.ARROW);
}
