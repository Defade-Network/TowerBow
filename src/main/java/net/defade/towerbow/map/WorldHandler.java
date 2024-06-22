package net.defade.towerbow.map;

import it.unimi.dsi.fastutil.Pair;
import net.defade.towerbow.game.GameInstance;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.WorldBorder;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;

public class WorldHandler implements Generator {
    /* The Long is the timestamp of the block. We want to check if the time
    has been elapsed so instead of checking the whole list we'll check only the first elements and stop once we reach a block that shouldn't be changed yet.
    The queue should thus be a FIFO queue.
    */
    private final Queue<Pair<Long, Point>> blockQueue = new PriorityQueue<>((a, b) -> (int) (a.left() - b.left()));

    private final GameInstance gameInstance;

    public WorldHandler(GameInstance gameInstance) {
        this.gameInstance = gameInstance;

        disableFloorBreaking();
        registerBlockDecay();
        registerWorldBorderDamage();

        gameInstance.setGenerator(this);
    }

    @Override
    public void generate(@NotNull GenerationUnit generationUnit) {
        // The zone is 100x100. Check if the generation unit is inside this zone or if it crosses it.
        Point absoluteStart = generationUnit.absoluteStart();
        Point absoluteEnd = generationUnit.absoluteEnd();

        // Check if the generation unit is inside the zone
        if (absoluteStart.blockX() >= -50 && absoluteEnd.blockX() <= 50 && absoluteStart.blockZ() >= -50 && absoluteEnd.blockZ() <= 50) {
            generationUnit.modifier().fillHeight(0, 1, Block.BLUE_STAINED_GLASS);
        } else {
            int startX = Math.max(-50, absoluteStart.blockX());
            int endX = Math.min(50, absoluteEnd.blockX());
            int startZ = Math.max(-50, absoluteStart.blockZ());
            int endZ = Math.min(50, absoluteEnd.blockZ());

            generationUnit.modifier().fill(new Pos(startX, 0, startZ), new Pos(endX, 1, endZ), Block.BLUE_STAINED_GLASS);
        }
    }

    /**
     * Register a block to be updated in the future.
     * @param pos The position of the block
     * @param time The time in milliseconds when the block should be updated
     */
    public void registerBlockDecay(Point pos, long time) {
        blockQueue.add(Pair.of(System.currentTimeMillis() + time, pos));
    }

    private void disableFloorBreaking() {
        gameInstance.getEventNode().getInstanceNode().addListener(PlayerBlockBreakEvent.class, event -> {
            if (event.getBlock().registry().material() == Material.BLUE_STAINED_GLASS) { // Don't allow players to break the floor
                event.setCancelled(true);
            }
        });
    }

    private void registerBlockDecay() {
        gameInstance.getEventNode().getInstanceNode()
                .addListener(PlayerBlockPlaceEvent.class, event -> {
                    event.consumeBlock(false);

                    // Register the block to be updated in the list.
                    blockQueue.add(Pair.of(System.currentTimeMillis() + 90 * 1000, event.getBlockPosition()));
                })
                .addListener(PlayerBlockBreakEvent.class, event -> {
                    // Remove the block from the list if it was placed
                    blockQueue.removeIf(pair -> pair.right().equals(event.getBlockPosition()));
                })
                .addListener(InstanceTickEvent.class, event -> {
                    long currentTime = System.currentTimeMillis();

                    while (!blockQueue.isEmpty()) {
                        Pair<Long, Point> block = blockQueue.peek();
                        if (block.left() > currentTime) {
                            break;
                        }

                        blockQueue.poll();

                        Block blockType = gameInstance.getBlock(block.right());

                        if (blockType.registry().material() == Material.COBBLESTONE) {
                            gameInstance.setBlock(block.right(), Block.MOSSY_COBBLESTONE);

                            // Add the block to the queue to be updated again in 90 seconds
                            blockQueue.add(Pair.of(currentTime + 90 * 1000, block.right()));
                        } else if (blockType.registry().material() == Material.MOSSY_COBBLESTONE) {
                            gameInstance.setBlock(block.right(), Block.AIR);
                        }
                    }
                });
    }

    private void registerWorldBorderDamage() {
        gameInstance.getEventNode().getInstanceNode().addListener(InstanceTickEvent.class, instanceTickEvent -> {
            Instance instance = instanceTickEvent.getInstance();
            WorldBorder worldBorder = instance.getWorldBorder();

            for (Player player : instance.getPlayers()) {
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
        });
    }

    public static void register(GameInstance gameInstance) {
        new WorldHandler(gameInstance);
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
