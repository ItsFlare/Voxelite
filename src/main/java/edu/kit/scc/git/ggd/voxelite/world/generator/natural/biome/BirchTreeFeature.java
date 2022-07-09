package edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec3i;

public class BirchTreeFeature implements TerrainFeature {

    @Override
    public boolean place(Voxel voxel) {
        int maxHeight = 12;
        int minHeight = 7;
        int height = (int) Math.floor(Math.random() * (maxHeight - minHeight + 1) + minHeight);
        Voxel relative;

        for (int i = 0; i < height; i++) {
            relative = voxel.getRelative(new Vec3i(0, i, 0));
            if(relative != null) relative.setBlock(Block.BIRCH_LOG);
        }

        for(int layer = -2; layer <= 2; layer++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if(!(x == 0 && z == 0)) {
                        if (!((x == 0 || z == 0) && Math.abs(layer) == 2)) {
                            relative = voxel.getRelative(new Vec3i(x < 0 ? x + Math.abs(layer) : x - Math.abs(layer), height - layer + 2, z < 0 ? z + Math.abs(layer) : z - Math.abs(layer)));
                            if(relative != null) relative.setBlock(Block.OAK_LEAVES);
                        }
                    }

                }
            }
        }

        relative = voxel.getRelative(new Vec3i(0, height, 0));
        if(relative != null) relative.setBlock(Block.OAK_LEAVES);

        return true;
    }
}
