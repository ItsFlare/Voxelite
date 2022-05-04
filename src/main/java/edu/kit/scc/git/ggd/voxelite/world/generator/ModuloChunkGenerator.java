package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.World;
import net.durchholz.beacon.math.Vec3i;

import java.util.concurrent.ThreadLocalRandom;

public class ModuloChunkGenerator implements WorldGenerator {
    private World world;
    public int    modulo = 16;

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public Chunk generate(Vec3i position) {
        Chunk chunk = new Chunk(world, position);

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Voxel voxel : chunk) {
            if((voxel.position().x() + voxel.position().y() + voxel.position().z()) % modulo == 0) voxel.setBlock(Block.values()[random.nextInt(Block.values().length)]);
        }

        return chunk;
    }
}
