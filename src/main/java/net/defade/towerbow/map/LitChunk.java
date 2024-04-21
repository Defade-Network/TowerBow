package net.defade.towerbow.map;

import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Section;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * Represents a chunk that is fully lit.
 */
public class LitChunk extends DynamicChunk {
    public LitChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        super(instance, chunkX, chunkZ);

        // Set all blocks to be fully lit
        byte[] fullLight = new byte[2048];
        Arrays.fill(fullLight, (byte) ((15 << 4) + 15)); // one byte holds the light level for two blocks

        for (Section section : sections) {
            section.blockLight().set(fullLight);
            section.skyLight().set(fullLight);
        }
    }
}
