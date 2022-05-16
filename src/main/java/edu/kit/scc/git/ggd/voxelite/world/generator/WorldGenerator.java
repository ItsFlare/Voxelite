package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.World;
import edu.kit.scc.git.ggd.voxelite.world.generator.pass.GeneratorPass;
import net.durchholz.beacon.math.Vec3i;

import java.util.List;

public interface WorldGenerator {

    void setWorld(World world);

    List<GeneratorPass> getPasses();

    Chunk generate(Vec3i position);

}
