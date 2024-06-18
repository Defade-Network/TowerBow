package net.defade.towerbow.teams;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;
import net.minestom.server.item.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a team in a game.
 * @param name the name of the team
 * @param color the color of the team
 * @param playerHelmet the helmet of the team. In tower bow, helmets are glasses blocks and needs to be of the same color as the team.
 */
public record Team(Component name, RGBLike color, Material playerHelmet) {
    private static final List<GameTeams> TEAMS = new ArrayList<>() {
        {
            add(new GameTeams(
                    new Team(Component.text("Red"), new Color(0xe74c3c), Material.RED_STAINED_GLASS),
                    new Team(Component.text("Blue"), new Color(0x3498db), Material.BLUE_STAINED_GLASS)
            ));
            add(new GameTeams(
                    new Team(Component.text("Cyan"), new Color(0x1abc9c), Material.CYAN_STAINED_GLASS),
                    new Team(Component.text("Yellow"), new Color(0xf1c40f), Material.YELLOW_STAINED_GLASS)
            ));
            add(new GameTeams(
                    new Team(Component.text("Orange"), new Color(0xfc7f11), Material.ORANGE_STAINED_GLASS),
                    new Team(Component.text("Purple"), new Color(0x9b59b6), Material.PURPLE_STAINED_GLASS)
            ));
        }
    };
    
    public static GameTeams getRandomTeams() {
        return TEAMS.get((int) (Math.random() * TEAMS.size()));
    }
}
