package edu.kit.scc.git.ggd.voxelite.world;

public class NaiveBlockStorage implements BlockStorage {
    private final Block[] blocks = new Block[Chunk.VOLUME];

    @Override
    public Block getBlock(int linear) {
        return blocks[linear];
    }

    @Override
    public void setBlock(int linear, Block block) {
        blocks[linear] = block;
    }
}
