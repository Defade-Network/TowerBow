package net.defade.towerbow.map;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

public class TowerBowMapGenerator implements Generator {
    @Override
    public void generate(@NotNull GenerationUnit generationUnit) {
        generationUnit.modifier().fillHeight(0, 1, Block.BLUE_STAINED_GLASS);
        // TODO: only generate a 100x100 area
    }
}
