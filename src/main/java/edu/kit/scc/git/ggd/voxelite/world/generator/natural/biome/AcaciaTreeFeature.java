package edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec3i;

public class AcaciaTreeFeature implements TerrainFeature {
    @Override
    public boolean place(Voxel voxel) {
        int maxHeight = 16;
        int minHeight = 12;
        int height = (int) Math.floor(Math.random() * (maxHeight - minHeight + 1) + minHeight);
        Voxel relative;



        for (int i = 0; i < height; i++) {
            relative = voxel.getRelative(new Vec3i(0, i, 0));
            if(relative != null) relative.setBlock(Block.ACACIA_LOG);
        }

        relative = voxel.getRelative(new Vec3i(0, height, 0));
        if(relative != null) relative.setBlock(Block.OAK_LEAVES);

        for (int layer = 0; layer < 2; layer++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (layer < 1) {
                        if(!(x == 0 && z == 0) && Math.abs(z) + Math.abs(x) != 2) {
                            relative = voxel.getRelative(new Vec3i(x, height - 1, z));
                            if(relative != null) relative.setBlock(Block.OAK_LEAVES);
                        }
                    } else {
                        if(!(x == 0 && z == 0)) {
                            relative = voxel.getRelative(new Vec3i(x, height - 3, z));
                            if(relative != null) relative.setBlock(Block.OAK_LEAVES);
                        }
                    }
                }
            }
        }
        int layerStart = height - 4;
        for (int layer = 0; layer < 3; layer++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if(!(x == 0 && z == 0)) {
                        relative = voxel.getRelative(new Vec3i(x < 0 ? x + layer : x - layer, layerStart - layer, z < 0 ? z + layer : z - layer));
                        if(relative != null) relative.setBlock(Block.OAK_LEAVES);
                    }
                }
            }
        }

        return true;
    }
}
