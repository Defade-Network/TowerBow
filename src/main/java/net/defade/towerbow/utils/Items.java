package net.defade.towerbow.utils;

import net.defade.towerbow.teams.Team;
import net.minestom.server.color.Color;
import net.minestom.server.item.Enchantment;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.LeatherArmorMeta;

import java.util.function.Function;

public class Items {
    public static final Function<Team, ItemStack> HELMET = team -> switch (team) {
        case ORANGE -> ItemStack.of(Material.ORANGE_STAINED_GLASS);
        case PURPLE -> ItemStack.of(Material.PURPLE_STAINED_GLASS);
    };

    public static final Function<Team, ItemStack> CHESTPLATE = team -> {
        Color teamColor = switch (team) {
            case ORANGE -> new Color(255, 165, 0);
            case PURPLE -> new Color(128, 0, 128);
        };

        return ItemStack.of(Material.LEATHER_CHESTPLATE).with(builder -> {
            builder.meta(new LeatherArmorMeta.Builder()
                    .color(teamColor)
                    .enchantment(Enchantment.PROTECTION, (short) 2)
                    .enchantment(Enchantment.FEATHER_FALLING, (short) 3)
                    .build());
        });
    };

    public static final ItemStack LEGGINGS = ItemStack.of(Material.IRON_LEGGINGS).withMeta(itemMeta -> {
        itemMeta.enchantment(Enchantment.PROTECTION, (short) 2);
        itemMeta.enchantment(Enchantment.FEATHER_FALLING, (short) 3);
    });

    public static final ItemStack BOOTS = ItemStack.of(Material.IRON_BOOTS).withMeta(itemMeta -> {
        itemMeta.enchantment(Enchantment.PROTECTION, (short) 2);
        itemMeta.enchantment(Enchantment.FEATHER_FALLING, (short) 3);
    });

    public static final ItemStack GOLDEN_APPLE = ItemStack.of(Material.GOLDEN_APPLE);

    public static final ItemStack GOLDEN_PICKAXE = ItemStack.of(Material.GOLDEN_PICKAXE).withMeta(itemMeta -> {
        itemMeta.enchantment(Enchantment.EFFICIENCY, (short) 2);
        itemMeta.enchantment(Enchantment.SHARPNESS, (short) 1);
    });

    public static final ItemStack BOW = ItemStack.of(Material.BOW).withMeta(itemMeta -> {
        itemMeta.enchantment(Enchantment.PUNCH, (short) 1);
        itemMeta.enchantment(Enchantment.POWER, (short) 2);
    });
}
