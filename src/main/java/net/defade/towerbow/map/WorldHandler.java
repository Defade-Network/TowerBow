package net.defade.towerbow.map;

import it.unimi.dsi.fastutil.Pair;
import net.defade.towerbow.game.GameInstance;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayDeque;
import java.util.Queue;

public class WorldHandler implements Generator {
    private final Queue<Pair<Long, Point>> blockQueue = new ArrayDeque<>(); /* The Long is the timestamp of the block. We want to check if the time
    has been elapsed so instead of checking the whole list we'll check only the first elements and stop once we reach a block that shouldn't be changed yet.
    The queue should thus be a FIFO queue.
    */

    public WorldHandler(GameInstance gameInstance) {
        gameInstance.getEventNode().getInstanceNode()
                .addListener(PlayerBlockBreakEvent.class, event -> {
                    if (event.getBlock().registry().material() == Material.BLUE_STAINED_GLASS) { // Don't allow players to break the floor
                        event.setCancelled(true);
                    }
                })
                .addListener(PlayerBlockPlaceEvent.class, event -> {
                    event.consumeBlock(false);

                    // Register the block to be updated in the list.
                    blockQueue.add(Pair.of(System.currentTimeMillis() + 3 * 1000, event.getBlockPosition()));
                }).addListener(InstanceTickEvent.class, event -> {
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
                            blockQueue.add(Pair.of(currentTime + 3 * 1000, block.right()));
                        } else if (blockType.registry().material() == Material.MOSSY_COBBLESTONE) {
                            gameInstance.setBlock(block.right(), Block.AIR);
                        }
                    }
                });

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

    public static void register(GameInstance gameInstance) {
        new WorldHandler(gameInstance);
    }
}
