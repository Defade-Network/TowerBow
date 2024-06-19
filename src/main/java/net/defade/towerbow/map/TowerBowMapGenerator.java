package net.defade.towerbow.map;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

public class TowerBowMapGenerator implements Generator {
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
}
