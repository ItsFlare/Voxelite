package edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass;

import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.MultiPassGenerator;

public interface GeneratorPass<G extends MultiPassGenerator<G>> {

    int ordinal();

    GeneratorPass<G>[] getValues();

    void apply(G generator, GeneratorChunk<G> chunk);

    default boolean isFirst() {
        return ordinal() == 0;
    }

    default boolean isLast() {
        return ordinal() == getValues().length - 1;
    }

    default GeneratorPass<G> getParent() {
        return isFirst() ? null : getValues()[ordinal() - 1];
    }

    default GeneratorPass<G> getChild() {
        return isLast() ? null : getValues()[ordinal() + 1];
    }
}
