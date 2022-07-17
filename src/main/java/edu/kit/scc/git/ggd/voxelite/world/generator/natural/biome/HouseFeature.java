package edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec3i;

public class HouseFeature implements TerrainFeature {
    @Override
    public boolean place(Voxel voxel) {
        int size = 5;

        Voxel relative;



        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y < 7; y++) {
                    relative = voxel.getRelative(new Vec3i(x, y, z));
                    if (relative == null) {
                        return false;
                    }
                }
            }
        }

        for (int x = -2; x <= 2; x++) {
            for(int z = -2; z <= 2; z++) {

                relative = voxel.getRelative(new Vec3i(x, 0, z));
                if(relative != null) relative.setBlock(Block.COBBLESTONE);

                if(Math.abs(x) == 2 || Math.abs(z) == 2) {
                    int counter = 0;
                    while (counter < size) {
                        relative = voxel.getRelative(new Vec3i(x, counter, z));
                        if(relative != null) {
                            if(Math.abs(x) + Math.abs(z) == 4) {
                                relative.setBlock(Block.OAK_LOG);
                            } else {
                                relative.setBlock(Block.COBBLESTONE);
                            }
                        }
                        counter++;
                    }
                    counter = -1;
                    relative = voxel.getRelative(new Vec3i(x, counter, z));
                    while (relative.getBlock() == Block.AIR) {
                        relative.setBlock(Block.COBBLESTONE);
                        counter--;
                        relative = voxel.getRelative(new Vec3i(x, counter, z));
                    }
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            for (int x = -2 + y; x <= 2 - y; x++) {
                for (int z = -2 + y; z <= 2 - y; z++) {
                    relative = voxel.getRelative(new Vec3i(x, size + y, z));
                    if(relative != null) relative.setBlock(Block.COBBLESTONE);
                }
            }
        }
        return true;
    }
}
