package edu.kit.scc.git.ggd.voxelite.world.generator.pass;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;

public interface GeneratorPass {

    void apply(Chunk chunk);

    void setFrequency(float frequency);

    void setAmplitude(int amplitude);

}
