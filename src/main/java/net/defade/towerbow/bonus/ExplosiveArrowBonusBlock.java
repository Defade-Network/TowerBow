package net.defade.towerbow.bonus;

import net.defade.towerbow.game.GameInstance;
import net.defade.towerbow.map.WorldHandler;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityShootEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;

public class ExplosiveArrowBonusBlock implements BonusBlock {
    private static final Tag<Boolean> EXPLOSIVE_ARROW = Tag.Boolean("explosive_arrow");

    @Override
    public void onHit(Player shooter) {
        shooter.setTag(EXPLOSIVE_ARROW, true);
    }

    @Override
    public void registerMechanics(GameInstance gameInstance) {
        gameInstance.getEventNode().getEntityInstanceNode()
                .addListener(EntityShootEvent.class, entityShootEvent -> {
                    boolean isExplosiveArrow = entityShootEvent.getEntity().hasTag(EXPLOSIVE_ARROW) && entityShootEvent.getEntity().getTag(EXPLOSIVE_ARROW);
                    entityShootEvent.getProjectile().setTag(EXPLOSIVE_ARROW, isExplosiveArrow);

                    entityShootEvent.getEntity().setTag(EXPLOSIVE_ARROW, false);
                })
                .addListener(ProjectileCollideWithBlockEvent.class, projectileCollideWithBlockEvent -> {
                    boolean isExplosiveArrow = projectileCollideWithBlockEvent.getEntity().hasTag(EXPLOSIVE_ARROW) && projectileCollideWithBlockEvent.getEntity().getTag(EXPLOSIVE_ARROW);
                    projectileCollideWithBlockEvent.getEntity().setTag(EXPLOSIVE_ARROW, false); // Remove it so that it doesn't explode again

                    if (!isExplosiveArrow) return;

                    // Get all blocks in a 3x3x3 cube around the hit block
                    int blockX = projectileCollideWithBlockEvent.getCollisionPosition().blockX();
                    int blockY = projectileCollideWithBlockEvent.getCollisionPosition().blockY();
                    int blockZ = projectileCollideWithBlockEvent.getCollisionPosition().blockZ();

                    for (int x = blockX - 1; x <= blockX + 1; x++) {
                        for (int y = blockY - 1; y <= blockY + 1; y++) {
                            for (int z = blockZ - 1; z <= blockZ + 1; z++) {
                                Block blockType = gameInstance.getBlock(x, y, z);
                                if (blockType == Block.AIR || blockType == Block.BLUE_STAINED_GLASS) continue;

                                gameInstance.setBlock(x, y, z, Block.MOSSY_COBBLESTONE);
                                WorldHandler worldHandler = (WorldHandler) gameInstance.generator();
                                // Should never be null
                                if (worldHandler != null) worldHandler.registerBlockDecay(new Pos(x, y, z), 2 * 1000); // 2 seconds
                            }
                        }
                    }
                });
    }
}
