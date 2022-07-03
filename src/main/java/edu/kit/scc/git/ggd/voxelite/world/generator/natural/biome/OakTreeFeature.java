package edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec3i;

public class OakTreeFeature implements TerrainFeature {
    @Override
    public boolean place(Voxel voxel) {
        int height = 6;

        for (int i = 0; i < height; i++) {
            final Voxel relative = voxel.getRelative(new Vec3i(0, i, 0));
            if(relative != null) relative.setBlock(Block.OAK_LOG);
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if(!(x == 0 && z == 0) && Math.abs(z) + Math.abs(x) != 4) {
                    for (int y = 0; y < 2; y++) {
                        final Voxel relative = voxel.getRelative(new Vec3i(x, height - 2 - y, z));
                        if(relative != null) relative.setBlock(Block.OAK_LEAVES);
                    }
                }
            }
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if(Math.abs(z) + Math.abs(x) != 2) {
                    final Voxel relative = voxel.getRelative(new Vec3i(x, height - 1, z));
                    if(relative != null) relative.setBlock(Block.OAK_LEAVES);
                }
            }
        }

        return true;
    }
}
