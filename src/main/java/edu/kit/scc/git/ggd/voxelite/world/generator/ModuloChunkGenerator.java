package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.World;
import edu.kit.scc.git.ggd.voxelite.world.WorldChunk;
import net.durchholz.beacon.math.Vec3i;

import java.util.concurrent.ThreadLocalRandom;

public class ModuloChunkGenerator implements WorldGenerator {
    public static final int DEFAULT_MOD = 16;

    private World world;
    public  int   modulo = DEFAULT_MOD;

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    public WorldChunk generate(Vec3i position) {
        final WorldChunk chunk = new WorldChunk(world, position);
        for (Voxel voxel : chunk) {
            if ((voxel.position().x() + voxel.position().y() + voxel.position().z()) % modulo == 0) {
                final ThreadLocalRandom random = ThreadLocalRandom.current();
                final Block block = Block.values()[random.nextInt(Block.values().length)];
                voxel.setBlock(block);
            }
        }

        return chunk;
    }
}
