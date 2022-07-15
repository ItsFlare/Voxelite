package edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec3i;

public class BirchTreeFeature implements TerrainFeature {

    @Override
    public boolean place(Voxel voxel) {
        int maxHeight = 17;
        int minHeight = 12;
        int height = (int) Math.floor(Math.random() * (maxHeight - minHeight + 1) + minHeight);
        Voxel relative;
        float randThreshhold = 0.5f;

        for (int i = 0; i < height; i++) {
            relative = voxel.getRelative(new Vec3i(0, i, 0));
            if(relative != null) relative.setBlock(Block.BIRCH_LOG);
        }

        for (int layer = 3; layer > 0; layer--) {
            for (int x = -layer; x <= layer; x++) {
                for (int z = -layer; z <= layer; z++) {
                    if(!(x == 0 && z == 0)) {
                        int start = 3;
                        if (layer == 3) {
                            relative = voxel.getRelative(new Vec3i(x, height - start, z));
                            if ((Math.abs(x) + Math.abs(z) == 2 * layer) && Math.random() > randThreshhold) {
                                continue;
                            }
                            if(relative != null && relative.getBlock() != Block.BIRCH_LOG) relative.setBlock(Block.OAK_LEAVES);
                        } else {
                            int offset = 3 - layer;

                            if ((Math.abs(x) + Math.abs(z) == 2 * layer) && Math.random() > randThreshhold) {
                                continue;
                            }
                            relative = voxel.getRelative(new Vec3i(x, height - start + offset, z));
                            if(relative != null && relative.getBlock() != Block.BIRCH_LOG) relative.setBlock(Block.OAK_LEAVES);
                            relative = voxel.getRelative(new Vec3i(x, height - start - offset, z));
                            if(relative != null && relative.getBlock() != Block.BIRCH_LOG) relative.setBlock(Block.OAK_LEAVES);
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
