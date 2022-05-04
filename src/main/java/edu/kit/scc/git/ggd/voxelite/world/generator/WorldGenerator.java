package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.World;
import net.durchholz.beacon.math.Vec3i;

public interface WorldGenerator {

    void setWorld(World world);

    Chunk generate(Vec3i position);

}
