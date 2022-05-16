package edu.kit.scc.git.ggd.voxelite.world;

public interface BlockStorage {
    Block getBlock(int linear);
    void setBlock(int linear, Block block);
}
