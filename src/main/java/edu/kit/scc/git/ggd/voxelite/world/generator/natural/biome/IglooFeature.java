package edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec3i;

public class IglooFeature implements TerrainFeature {

    @Override
    public boolean place(Voxel voxel) {
        Voxel relative;

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y < 7; y++) {
                    relative = voxel.getRelative(new Vec3i(x, y, z));
                    if (relative == null) {
                        return false;
                    }
                }
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                relative = voxel.getRelative(new Vec3i(x, 0, z));
                if(relative != null) {
                    relative.setBlock(Block.COBBLESTONE); //TODO replace with snow
                }

                for (int y = 1; y <= 3; y++) {
                    if (Math.abs(x) == 3 - y) {
                        if (y == 1) {
                            voxel.getRelative(new Vec3i(x, y, z)).setBlock(Block.COBBLESTONE);
                        }
                        if (z == -3) {
                            int num = y - 1;
                            while(num >= 0) {
                                voxel.getRelative(new Vec3i(x,y - num,z)).setBlock(Block.COBBLESTONE);
                                num--;
                            }
                        }
                        voxel.getRelative(new Vec3i(x, y + 1, z)).setBlock(Block.COBBLESTONE);
                    }
                }
            }
        }
        return true;
    }
}
