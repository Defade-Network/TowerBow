package net.defade.towerbow.teams;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.utils.Items;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.sound.SoundEvent;

import java.util.ArrayList;
import java.util.List;

public class TeamSelectorGUI extends Inventory {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final GameInstance gameInstance;

    public TeamSelectorGUI(GameInstance gameInstance) {
        super(InventoryType.CHEST_3_ROW, Component.text("Sélecteur d'équipe"));
        this.gameInstance = gameInstance;

        for (int i = 0; i < 3 * 9; i++) {
            setItemStack(i, Items.GUI_FILLER);
        }

        setItemStack(13, Items.RANDOM_TEAM);
        updateItems();

        addInventoryCondition((player, slot, clickType, inventoryConditionResult) -> {
            inventoryConditionResult.setCancel(true);

            Team team = switch (slot) {
                case 11 -> gameInstance.getTeams().firstTeam();
                case 13 -> null;
                case 15 -> gameInstance.getTeams().secondTeam();
                default -> player.getTeam();
            };

            if (player.getTeam() == team) return;

            if (team != null) {
                if (TeamUtils.isTeamFull(team)) {
                    player.sendMessage(Component.text("Cette équipe est pleine"));
                    player.playSound(Sound.sound().type(SoundEvent.BLOCK_ANVIL_LAND).pitch(1.5F).volume(1F).build(), player.getPosition());
                    return;
                }
            }

            player.setTeam(team);

            if (slot == 11 || slot == 15) {
                player.sendMessage(MM.deserialize(
                        "<gray>»</gray> <yellow>Vous avez rejoint l'équipe</yellow> <team> <yellow>!</yellow>",
                        Placeholder.component("team", player.getTeam().getTeamDisplayName()))
                );
                player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_PLING).pitch(2F).volume(1F).build(), player.getPosition());
            } else if (slot == 13) {
                player.sendMessage(MM.deserialize(
                        "<gray>»</gray> <yellow>Vous avez rejoint une équipe <gradient:#DE3472:#F2C525>aléatoire</gradient>!</yellow>"
                ));
                player.playSound(Sound.sound().type(SoundEvent.BLOCK_NOTE_BLOCK_PLING).pitch(2F).volume(1F).build(), player.getPosition());
            }

            updateItems();
        });
    }

    private void updateItems() {
        setItemStack(11, getItemForTeam(gameInstance.getTeams().firstTeam()));
        setItemStack(15, getItemForTeam(gameInstance.getTeams().secondTeam()));
    }

    private static ItemStack getItemForTeam(Team team) {
        ItemStack teamItem = Items.HELMET.apply(team);

        teamItem = teamItem.with(ItemComponent.ITEM_NAME,
                MM.deserialize(
                        "<!i><team_color><team> </team_color><gray>(<team_count>/<team_total>)",
                        TagResolver.builder()
                                .resolver(Placeholder.styling("team_color", team.getTeamDisplayName().color()))
                                .resolver(Placeholder.component("team", team.getTeamDisplayName()))
                                .resolver(Placeholder.component("team_count", Component.text(team.getMembers().size())))
                                .resolver(Placeholder.component("team_total", Component.text(TeamUtils.MAX_PLAYERS_PER_TEAM)))
                                .build()
                )
        );

        List<Component> lore = new ArrayList<>();

        for (Player player : team.getPlayers()) {
            lore.add(MM.deserialize(
                    "<!i><dark_gray><b> - </b></dark_gray><player>",
                    Placeholder.component("player", player.getName().color(team.getTeamDisplayName().color()))
            ));
        }

        return teamItem.withLore(lore);
    }
}
