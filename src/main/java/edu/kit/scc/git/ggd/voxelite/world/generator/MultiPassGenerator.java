package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass.GeneratorPass;

public interface MultiPassGenerator<G extends MultiPassGenerator<G>> extends WorldGenerator {

    GeneratorPass<G>[] getPasses();

    default GeneratorPass<G> getFirstPass() {
        return getPasses()[0];
    }
}
