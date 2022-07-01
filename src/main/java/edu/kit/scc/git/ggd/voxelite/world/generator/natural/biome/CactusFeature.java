package edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec3i;

public class CactusFeature implements TerrainFeature {

    @Override
    public boolean place(Voxel voxel) {

        for (int z = 0; z < 4; z++) {
            voxel.getRelative(new Vec3i(0, z, 0)).setBlock(Block.OAK_LOG);
        }

        return true;
    }
}
