package edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass;

import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.NaturalWorldGenerator;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.SimplexNoise;

public class CavePass implements GeneratorPassInstance<NaturalWorldGenerator> {
    private final Noise noise;
    private final NaturalWorldGenerator generator;

    public CavePass(NaturalWorldGenerator generator) {
        this.generator = generator;
        this.noise = new SimplexNoise(generator.getSeed());
    }

    @Override
    public void apply(GeneratorChunk<NaturalWorldGenerator> chunk) {

    }
}