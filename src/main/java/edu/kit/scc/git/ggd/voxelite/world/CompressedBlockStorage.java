package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.util.Util;

public class CompressedBlockStorage implements BlockStorage {
    private static final int  BITS_PER_BLOCK_EXP  = Util.log2(Util.log2(Block.values().length));
    private static final int  BITS_PER_BLOCK      = 1 << BITS_PER_BLOCK_EXP;
    private static final int  BLOCKS_PER_ELEMENT  = (64 >>> BITS_PER_BLOCK_EXP);
    private static final long MASK                = (1L << BITS_PER_BLOCK) - 1;

    private final long[] blocks = new long[Chunk.VOLUME >>> BITS_PER_BLOCK];

    @Override
    public Block getBlock(int linear) {
        int index = index(linear);
        int shift = shift(linear);
        return Block.values()[(int) ((blocks[index] >>> shift) & MASK)];
    }

    @Override
    public void setBlock(int linear, Block block) {
        int index = index(linear);
        int shift = shift(linear);
        blocks[index] &= ~(MASK << shift);
        blocks[index] |= (long) block.ordinal() << shift;
    }

    private static int index(int linear) {
        return linear >>> BITS_PER_BLOCK;
    }

    private static int shift(int linear) {
        return (linear & (BLOCKS_PER_ELEMENT - 1)) << BITS_PER_BLOCK_EXP;
    }
}
