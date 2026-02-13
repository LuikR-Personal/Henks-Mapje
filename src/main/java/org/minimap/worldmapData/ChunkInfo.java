package org.minimap.worldmapData;
// stores info about discovered chunks. Used in rendering
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.map.MapPalette;

import java.awt.Color;

public class ChunkInfo {
    public final int chunkX;
    public final int chunkZ;
    public final int topY;
    public final Material topMaterial;
    public final Biome biome;
    private final int[] sampleRgb;
    private byte[] samplePalette;

    public ChunkInfo(int chunkX, int chunkZ, int topY, Material topMaterial, Biome biome) {
        this(chunkX, chunkZ, topY, topMaterial, biome, null);
    }

    public ChunkInfo(int chunkX, int chunkZ, int topY, Material topMaterial, Biome biome, int[] sampleRgb) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.topY = topY;
        this.topMaterial = topMaterial;
        this.biome = biome;
        this.sampleRgb = sampleRgb;
    }

    public int[] sampleRgb() {
        return sampleRgb;
    }

    public synchronized byte[] samplePalette() {
        if (sampleRgb == null) {
            return null;
        }
        if (samplePalette == null) {
            samplePalette = new byte[sampleRgb.length];
            for (int i = 0; i < sampleRgb.length; i++) {
                int rgb = sampleRgb[i];
                Color color = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                samplePalette[i] = MapPalette.matchColor(color);
            }
        }
        return samplePalette;
    }
}
