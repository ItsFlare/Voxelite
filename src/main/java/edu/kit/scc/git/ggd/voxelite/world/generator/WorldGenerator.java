package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.World;

public interface WorldGenerator {

    World getWorld();
    void setWorld(World world);
}
