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
import net.defade.towerbow.teams.TeamsManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

public class FightHandler {
    private static final Tag<Pos> PLAYER_SHOOT_POS = Tag.Structure("arrow_touched_ground", Pos.class); // Position at which the player shot the arrow
    private static final Tag<Boolean> ARROW_TOUCHED_GROUND = Tag.Boolean("arrow_touched_ground"); // Used to check if the arrow can be considered a longshot

    private final EventNode<EntityInstanceEvent> PVP_NODE = PvPConfig.emptyBuilder()
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
            .createNode()
            .addListener(EntityShootEvent.class, entityShootEvent -> {
                entityShootEvent.setSpread(0); // Disable arrow randomness
            })
            // Disable friendly fire
            .addListener(EntityDamageEvent.class, entityDamageEvent -> {
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
            })
            // Damage players below a certain height
            .addListener(PlayerTickEvent.class, playerTickEvent -> {
                if (playerTickEvent.getPlayer().getPosition().y() < 15) { // TODO: determine right height and damage
                    playerTickEvent.getPlayer().damage(
                            new Damage(
                                    DamageType.FALL,
                                    null,
                                    null,
                                    null,
                                    1
                            )
                    );
                }
            })
            // Longshots
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

                if (shooter == null || !arrow.hasTag(PLAYER_SHOOT_POS)) return; // Should never happen but just in case

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

    private final GameInstance gameInstance;

    public FightHandler(GameInstance gameInstance) {
        this.gameInstance = gameInstance;

        enableWorldBorderDamage();
    }

    public void enablePvp(boolean enable) {
        if (enable) {
            gameInstance.getEventNode().getEntityInstanceNode().addChild(PVP_NODE);
        } else {
            gameInstance.getEventNode().getEntityInstanceNode().removeChild(PVP_NODE);
        }
    }

    private void enableWorldBorderDamage() {
        gameInstance.scheduler().scheduleTask(() -> {
            WorldBorder worldBorder = gameInstance.getWorldBorder();

            for (Player player : gameInstance.getPlayers()) {
                if (!worldBorder.inBounds(player)) {

                    double closestDistanceToBorder = getClosestDistanceToBorder(player, worldBorder);

                    // If the player is within 5 blocks of the border, calculate the damage to be inflicted
                    double damageThreshold = closestDistanceToBorder + 5;
                    if (damageThreshold < 0) {
                        double damage = Math.max(1, Math.floor(-(damageThreshold) * 0.6));
                        player.damage(new Damage(
                                DamageType.OUTSIDE_BORDER,
                                null,
                                null,
                                null,
                                (float) damage
                        ));
                    }
                }
            }
        }, TaskSchedule.immediate(), TaskSchedule.tick(1));
    }

    private static double getClosestDistanceToBorder(Player player, WorldBorder worldBorder) {
        double radius = worldBorder.diameter() / 2;

        double distanceToEastBorder = player.getPosition().x() - (worldBorder.centerX() - radius);
        double distanceToWestBorder = (worldBorder.centerX() + radius) - player.getPosition().x();
        double distanceToNorthBorder = player.getPosition().z() - (worldBorder.centerZ() - radius);
        double distanceToSouthBorder = (worldBorder.centerZ() + radius) - player.getPosition().z();

        // Find the minimum distance to the border
        return Math.min(
                Math.min(distanceToWestBorder, distanceToEastBorder),
                Math.min(distanceToNorthBorder, distanceToSouthBorder)
        );
    }
}
