package edu.kit.scc.git.ggd.voxelite.world.generator.pass;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.SimplexNoise;

public class TerrainPass implements GeneratorPass {
    private final Noise noise;

    public TerrainPass(long seed) {
        this.noise = new SimplexNoise(seed);
    }

    @Override
    public void apply(Chunk chunk) {
        //TODO Implement
    }
}
