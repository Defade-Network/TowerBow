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
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;

public class FightHandler {
    private static final EventNode<EntityInstanceEvent> PVP_NODE = PvPConfig.emptyBuilder()
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
            .addListener(EntityDamageEvent.class, entityDamageEvent -> { // Disable friendly fire
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

    private final GameInstance gameInstance;

    public FightHandler(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    public void enablePvp(boolean enable) {
        if (enable) {
            gameInstance.getEventNode().getEntityInstanceNode().addChild(PVP_NODE);
        } else {
            gameInstance.getEventNode().getEntityInstanceNode().removeChild(PVP_NODE);
        }
    }
}
