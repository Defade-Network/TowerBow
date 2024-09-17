package net.defade.towerbow.utils;

import io.github.togar2.pvp.player.CombatPlayerImpl;
import net.defade.towerbow.fight.CombatMechanics;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TowerBowPlayer extends CombatPlayerImpl {
    public TowerBowPlayer(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection playerConnection) {
        super(uuid, username, playerConnection);
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        return super.setInstance(instance, spawnPosition).thenRun(() -> {
            setFoodSaturation(0);
            setEnableRespawnScreen(false);

            setGameMode(GameMode.SURVIVAL);

            CombatMechanics.setRemainingLives(this, 3); // TODO config
        });
    }

    @Override
    public void setTeam(@Nullable Team team) {
        super.setTeam(team);

        if (team != null) {
            setDisplayName(getName().color(team.getTeamDisplayName().color()));
        } else {
            setDisplayName(getName().color(NamedTextColor.WHITE));
        }
    }
}
