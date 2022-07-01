package edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass;

import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.MultiPassGenerator;

public interface GeneratorPassInstance<G extends MultiPassGenerator<G>> {
    void apply(GeneratorChunk<G> chunk);
}
