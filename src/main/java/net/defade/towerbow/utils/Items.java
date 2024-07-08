package net.defade.towerbow.utils;

import net.defade.towerbow.teams.TeamUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.DyedItemColor;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.component.HeadProfile;
import net.minestom.server.item.component.Unbreakable;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.scoreboard.Team;
import java.util.Map;
import java.util.function.Function;

public class Items {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static final ItemStack TEAM_SELECTOR = ItemStack.of(Material.ENDER_EYE)
            .with(ItemComponent.ITEM_NAME, MM.deserialize("<dark_gray>» </dark_gray><gradient:#3EB835:#B2EA80>Sélecteur d'équipe</gradient>"));

    public static final ItemStack GUI_FILLER = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE)
            .with(ItemComponent.HIDE_TOOLTIP);

    public static final ItemStack RANDOM_TEAM = ItemStack.of(Material.PLAYER_HEAD)
            .with(ItemComponent.ITEM_NAME, MM.deserialize("<gray>»</gray><white> Aléatoire </white><gray>«</gray>"))
            .with(ItemComponent.PROFILE,
                    new HeadProfile(new PlayerSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh" +
                    "0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGEwODRkMGExYzZmYzIxNjNkZTMwZDhiMTQ4YWI0ZDM2MzIyMGQ1" +
                    "Yzk3MmQ1Zjg4ZWI4ZGM4NjE3NmNjZGIzZSJ9fX0=", ""))
            );

    public static final Function<Team, ItemStack> HELMET = team -> ItemStack.of(TeamUtils.getPlayerHelmetForTeam(team));

    public static final Function<Team, ItemStack> CHESTPLATE = team -> {
        DyedItemColor teamColor = new DyedItemColor(team.getTeamDisplayName().color());

        return ItemStack.of(Material.LEATHER_CHESTPLATE)
                .with(ItemComponent.DYED_COLOR, teamColor)
                .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                        Map.of(
                                Enchantment.PROTECTION, 2
                        )
                ))
                .with(ItemComponent.HIDE_TOOLTIP);
    };

    public static final ItemStack LEGGINGS = ItemStack.of(Material.IRON_LEGGINGS)
            .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                    Map.of(
                            Enchantment.PROTECTION, 2
                    )
            ))
            .with(ItemComponent.HIDE_TOOLTIP);

    public static final ItemStack BOOTS = ItemStack.of(Material.IRON_BOOTS)
            .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                    Map.of(
                            Enchantment.PROTECTION, 2,
                            Enchantment.FEATHER_FALLING, 6
                    )
            ))
            .with(ItemComponent.HIDE_TOOLTIP);

    public static final ItemStack GOLDEN_APPLE = ItemStack.of(Material.GOLDEN_APPLE);

    public static final ItemStack GOLDEN_PICKAXE = ItemStack.of(Material.GOLDEN_PICKAXE)
            .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                    Map.of(
                            Enchantment.EFFICIENCY, 2,
                            Enchantment.SHARPNESS, 4
                    )
            ))
            .with(ItemComponent.UNBREAKABLE, new Unbreakable(false))
            .with(ItemComponent.HIDE_TOOLTIP);

    public static final ItemStack BOW = ItemStack.of(Material.BOW)
            .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                    Map.of(
                            Enchantment.INFINITY, 1
                    )
            ))
            .with(ItemComponent.UNBREAKABLE, new Unbreakable(false));

    public static final ItemStack ARROW = ItemStack.of(Material.ARROW);

    public static final ItemStack COBBLESTONE = ItemStack.of(Material.COBBLESTONE).withAmount(3);
}
