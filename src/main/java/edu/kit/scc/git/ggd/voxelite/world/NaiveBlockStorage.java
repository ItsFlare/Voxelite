package edu.kit.scc.git.ggd.voxelite.world;

import java.util.Arrays;
import java.util.Collections;

public class NaiveBlockStorage implements ChunkStorage<Block> {
    private static final Block[] EMPTY = Collections.nCopies(Chunk.VOLUME, Block.AIR).toArray(Block[]::new);

    private final Block[] blocks = Arrays.copyOf(EMPTY, EMPTY.length);

    @Override
    public Block get(int linear) {
        return blocks[linear];
    }

    @Override
    public void set(int linear, Block block) {
        blocks[linear] = block;
    }
}
