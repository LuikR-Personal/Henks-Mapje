package org.minimap.Services;

import org.bukkit.Material;
import org.bukkit.World;

import java.awt.Color;

public final class ChunkSampling {

    public static final int SAMPLE_GRID = 4;

    private ChunkSampling() {
    }

    public static int[] sampleChunkRgb(World world, int chunkX, int chunkZ) {
        int[] samples = new int[SAMPLE_GRID * SAMPLE_GRID];
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int minY = world.getMinHeight();
        int index = 0;

        for (int sz = 0; sz < SAMPLE_GRID; sz++) {
            for (int sx = 0; sx < SAMPLE_GRID; sx++) {
                int worldX = baseX + 2 + (sx * 4);
                int worldZ = baseZ + 2 + (sz * 4);
                int topY = world.getHighestBlockYAt(worldX, worldZ);
                int blockY = Math.max(minY, topY - 1);
                Material material = world.getBlockAt(worldX, blockY, worldZ).getType();
                Color color = WorldmapRenderer.baseColorFor(material);
                samples[index++] = (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
            }
        }

        return samples;
    }
}
