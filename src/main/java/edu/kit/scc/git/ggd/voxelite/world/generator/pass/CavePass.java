package edu.kit.scc.git.ggd.voxelite.world.generator.pass;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.SimplexNoise;
import java.util.Iterator;
import net.durchholz.beacon.math.Vec3f;

public class CavePass implements GeneratorPass {
    private final Noise noise;
    private int minHeight = -128;

    public CavePass(long seed) {
        this.noise = new SimplexNoise(seed);
    }

    public void apply(Chunk chunk) {
        int offset = -5;
        int offset2 = -50;
        int offset3 = -20;
        float range = 0.135F;
        Iterator var6 = chunk.iterator();

        while(true) {
            Voxel voxel;
            do {
                if (!var6.hasNext()) {
                    return;
                }

                voxel = (Voxel)var6.next();
            } while(voxel.position().y() >= offset);

            Vec3f pos2 = new Vec3f((float)voxel.position().x(), (float)voxel.position().y(), (float)voxel.position().z());
            double density = this.noise.sample(pos2.scale(0.05F));
            if (Math.abs(density) <= (double)range && !this.checkWater(voxel)) {
                voxel.setBlock(Block.AIR);
            } else if (Math.abs(density) <= (double)range && voxel.position().y() < offset3) {
                voxel.setBlock(Block.STONE);
            }

            if (density < 0.0 && Math.abs(density) > (double)range && voxel.position().y() < offset2) {
                voxel.setBlock(Block.AIR);
            }

            if (voxel.position().y() < this.minHeight) {
                voxel.setBlock(Block.AIR);
            }

            if (voxel.position().y() == this.minHeight) {
                voxel.setBlock(Block.BEDROCK);
            }
        }
    }

    public void setFrequency(float frequency) {
    }

    public void setAmplitude(int amplitude) {
    }

    private boolean checkWater(Voxel voxel) {
        int range = 3;
        Voxel voxelPosX = voxel.getNeighbor(Direction.POS_X);
        Voxel voxelPosY = voxel.getNeighbor(Direction.POS_Y);
        Voxel voxelPosZ = voxel.getNeighbor(Direction.POS_Z);
        Voxel voxelNegX = voxel.getNeighbor(Direction.NEG_X);
        Voxel voxelNegY = voxel.getNeighbor(Direction.NEG_Y);
        Voxel voxelNegZ = voxel.getNeighbor(Direction.NEG_Z);

        for(int i = 0; i < range; ++i) {
            if (voxelPosX != null) {
                if (voxelPosX.getBlock().equals(Block.COBBLESTONE)) {
                    return true;
                }

                voxelPosX = voxelPosX.getNeighbor(Direction.POS_X);
            }

            if (voxelPosY != null) {
                if (voxelPosY.getBlock().equals(Block.COBBLESTONE)) {
                    return true;
                }

                voxelPosY = voxelPosY.getNeighbor(Direction.POS_Y);
            }

            if (voxelPosZ != null) {
                if (voxelPosZ.getBlock().equals(Block.COBBLESTONE)) {
                    return true;
                }

                voxelPosZ = voxelPosZ.getNeighbor(Direction.POS_Z);
            }

            if (voxelNegX != null) {
                if (voxelNegX.getBlock().equals(Block.COBBLESTONE)) {
                    return true;
                }

                voxelNegX = voxelNegX.getNeighbor(Direction.NEG_X);
            }

            if (voxelNegY != null) {
                if (voxelNegY.getBlock().equals(Block.COBBLESTONE)) {
                    return true;
                }

                voxelNegY = voxelNegY.getNeighbor(Direction.NEG_Y);
            }

            if (voxelNegZ != null) {
                if (voxelNegZ.getBlock().equals(Block.COBBLESTONE)) {
                    return true;
                }

                voxelNegZ = voxelNegZ.getNeighbor(Direction.NEG_Z);
            }
        }

        return false;
    }
}