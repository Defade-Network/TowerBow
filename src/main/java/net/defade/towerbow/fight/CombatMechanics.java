package net.defade.towerbow.fight;

import io.github.togar2.pvp.config.ArmorToolConfig;
import io.github.togar2.pvp.config.AttackConfig;
import io.github.togar2.pvp.config.DamageConfig;
import io.github.togar2.pvp.config.FoodConfig;
import io.github.togar2.pvp.config.PotionConfig;
import io.github.togar2.pvp.config.ProjectileConfig;
import io.github.togar2.pvp.config.PvPConfig;
import io.github.togar2.pvp.projectile.AbstractArrow;
import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.game.state.GameState;
import net.defade.towerbow.teams.TeamsManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.tag.Tag;
import java.util.UUID;

public class CombatMechanics {
    public static final Tag<Integer> PLAYER_KILLS = Tag.Integer("kills");
    private static final Tag<UUID> LAST_DAMAGER_UUID = Tag.UUID("last_damager"); // Used to store the last player who damaged the player

    private static final Tag<Pos> PLAYER_SHOOT_POS = Tag.Structure("arrow_touched_ground", Pos.class); // Position at which the player shot the arrow
    private static final Tag<Boolean> ARROW_TOUCHED_GROUND = Tag.Boolean("arrow_touched_ground"); // Used to check if the arrow can be considered a longshot

    private final EventNode<EntityInstanceEvent> combatMechanicsNode;

    private CombatMechanics(GameInstance gameInstance) {
        this.combatMechanicsNode = createPvPNode();
        disableArrowSpread();
        disableFriendlyFire();

        registerLongShots();

        registerDeathHandler();

        registerKillCounter(gameInstance);
    }

    private EventNode<EntityInstanceEvent> createPvPNode() {
        return PvPConfig.emptyBuilder()
                // Enable all PvP features
                .attack(AttackConfig.defaultBuilder().attackCooldown(false).build())
                .armorTool(ArmorToolConfig.DEFAULT)
                .damage(DamageConfig.DEFAULT)
                .food(FoodConfig.emptyBuilder(false)
                        .eating(true)
                        .eatingSounds(true)
                        .naturalRegeneration(true)
                )
                .projectile(ProjectileConfig.emptyBuilder(false)
                        .bow(true)
                )
                .potion(PotionConfig.emptyBuilder(false)
                        .applyEffect(true)
                        .updateEffect(true)
                )
                .build()
                .createNode();
    }

    private void disableArrowSpread() {
        combatMechanicsNode.addListener(EntityShootEvent.class, entityShootEvent -> {
            entityShootEvent.setSpread(0); // Disable arrow randomness
        });
    }

    private void disableFriendlyFire() {
        combatMechanicsNode.addListener(EntityDamageEvent.class, entityDamageEvent -> {
            if (!(entityDamageEvent.getEntity() instanceof Player player)) return;

            Damage damageSource = entityDamageEvent.getDamage();
            Player damager;
            if (damageSource.getSource() instanceof Player) {
                damager = (Player) damageSource.getSource();
            } else if (damageSource.getSource() instanceof AbstractArrow arrow) {
                damager = (Player) arrow.getShooter();
            } else {
                return;
            }

            if (player == damager) return; // Allow self damage

            TeamsManager teamsManager = ((GameInstance) player.getInstance()).getTeams();

            if (teamsManager.getTeam(player) == teamsManager.getTeam(damager)) {
                entityDamageEvent.setCancelled(true);
            }
        });
    }

    private void registerLongShots() {
        combatMechanicsNode
                .addListener(EntityShootEvent.class, entityShootEvent -> {
                    entityShootEvent.getProjectile().setTag(PLAYER_SHOOT_POS, entityShootEvent.getEntity().getPosition());
                })
                .addListener(ProjectileCollideWithBlockEvent.class, projectileCollideWithBlockEvent -> {
                    projectileCollideWithBlockEvent.getEntity().setTag(ARROW_TOUCHED_GROUND, true);
                })
                .addListener(EntityDamageEvent.class, entityDamageEvent -> {
                    if (!(entityDamageEvent.getDamage().getSource() instanceof AbstractArrow arrow)) return;

                    if (arrow.hasTag(ARROW_TOUCHED_GROUND) && arrow.getTag(ARROW_TOUCHED_GROUND)) {
                        return; // Arrow touched the ground, can't be considered a longshot
                    }

                    Player shooter = (Player) arrow.getShooter();
                    Player target = (Player) entityDamageEvent.getEntity();

                    if (shooter == null || !arrow.hasTag(PLAYER_SHOOT_POS))
                        return; // Should never happen but just in case

                    Pos shootPos = arrow.getTag(PLAYER_SHOOT_POS);

                    double distance = shootPos.distance(target.getPosition());
                    if (distance > 50) { // If the distance is > 50 blocks then it's a longshot
                        shooter.setHealth(Math.min(shooter.getHealth() + 2, (float) shooter.getAttributeValue(Attribute.GENERIC_MAX_HEALTH)));

                        shooter.getInstance().sendMessage(
                                shooter.getDisplayName()
                                        .append(Component.text(" made a longshot of "))
                                        .append(Component.text((int) distance + " blocks!"))
                        );
                    }
                });
    }

    private void registerDeathHandler() {
        combatMechanicsNode.addListener(PlayerDeathEvent.class, playerDeathEvent -> {
            GameInstance gameInstance = (GameInstance) playerDeathEvent.getPlayer().getInstance();
            Player player = playerDeathEvent.getPlayer();

            player.setGameMode(GameMode.SPECTATOR);
            player.setRespawnPoint(player.getPosition());

            // Check if all the players of his team are dead
            boolean allPlayersInTeamDead = gameInstance.getTeams().getPlayers(gameInstance.getTeams().getTeam(player))
                    .stream()
                    .noneMatch(teamPlayer -> teamPlayer.getGameMode() != GameMode.SPECTATOR);

            if (allPlayersInTeamDead) {
                gameInstance.setGameState(GameState.FINISHED);
            }
        });
    }

    private void registerKillCounter(GameInstance gameInstance) {
        gameInstance.getPlayers().forEach(player -> player.setTag(PLAYER_KILLS, 0));

        combatMechanicsNode.addListener(EntityDamageEvent.class, entityDamageEvent -> {
            if (!(entityDamageEvent.getDamage().getSource() instanceof Player damager)) return;

            entityDamageEvent.getEntity().setTag(LAST_DAMAGER_UUID, damager.getUuid());
        }).addListener(PlayerDeathEvent.class, playerDeathEvent -> {
            Player killer = gameInstance.getPlayerByUuid(playerDeathEvent.getPlayer().getTag(LAST_DAMAGER_UUID));
            if (killer == null || killer == playerDeathEvent.getPlayer()) return; // Ignore self kills

            killer.setTag(PLAYER_KILLS, killer.getTag(PLAYER_KILLS) + 1);
        });
    }

    public static EventNode<EntityInstanceEvent> create(GameInstance gameInstance) {
        return new CombatMechanics(gameInstance).combatMechanicsNode;
    }
}
