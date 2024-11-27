package net.defade.towerbow.utils;

import net.defade.towerbow.teams.TeamUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.EquipmentSlotGroup;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.*;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.utils.NamespaceID;

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

    public static final ItemStack SPECTATE_ITEM = ItemStack.of(Material.ENDER_EYE)
            .with(ItemComponent.ITEM_NAME, MM.deserialize("<dark_gray>» </dark_gray><gradient:#3EB835:#B2EA80>Spectate</gradient>"));

    public static final ItemStack JOIN_NEW_GAME = ItemStack.of(Material.PAPER)
            .with(ItemComponent.ITEM_NAME, MM.deserialize("<bold><yellow>» <aqua>Rejoindre une nouvelle partie</aqua> <yellow>«</yellow></bold>"));

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
                            Enchantment.FEATHER_FALLING, 5
                    )
            ))
            .with(ItemComponent.HIDE_TOOLTIP);

    public static final ItemStack GOLDEN_APPLE = ItemStack.of(Material.GOLDEN_APPLE);

    public static final ItemStack GOLDEN_PICKAXE = ItemStack.of(Material.GOLDEN_PICKAXE)
            .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                    Map.of(
                            Enchantment.EFFICIENCY, 2,
                            Enchantment.SHARPNESS, 4,
                            Enchantment.KNOCKBACK, 1
                    ), false
            ))
            .with(ItemComponent.UNBREAKABLE, new Unbreakable(false))
            .with(ItemComponent.ATTRIBUTE_MODIFIERS,
                    new AttributeList(
                            new AttributeList.Modifier(
                                Attribute.GENERIC_ATTACK_SPEED,
                                new AttributeModifier(
                                        NamespaceID.from("defade:no_attack_cooldown"),
                                        10,
                                        AttributeOperation.ADD_VALUE),
                                EquipmentSlotGroup.ANY), false))
            .with(ItemComponent.HIDE_ADDITIONAL_TOOLTIP)
            .withLore(MM.deserialize(
                    "<!i><gray>Spamclick:</gray> <green>✔</green>"
            ));


    public static final ItemStack BOW = ItemStack.of(Material.BOW)
            .with(ItemComponent.ENCHANTMENTS, new EnchantmentList(
                    Map.of(
                            Enchantment.INFINITY, 1
                    )
            ))
            .with(ItemComponent.UNBREAKABLE, new Unbreakable(false));

    public static final ItemStack ARROW = ItemStack.of(Material.ARROW);

    public static final ItemStack COBBLESTONE = ItemStack.of(Material.COBBLESTONE)
            .withAmount(3)
            .withLore(MM.deserialize(
                    "<!i><gray>Infinite:</gray> <aqua>∞</aqua>"
            ));
}
