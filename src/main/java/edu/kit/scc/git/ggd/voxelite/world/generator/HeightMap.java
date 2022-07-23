package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;

public record HeightMap(Voxel[] data) {

    public HeightMap() {
        this(new Voxel[Chunk.WIDTH * Chunk.WIDTH]);
    }

    public Voxel get(int x, int z) {
        return data[x * Chunk.WIDTH + z];
    }

    public void set(int x, int z, Voxel height) {
        data[x * Chunk.WIDTH + z] = height;
    }
}
