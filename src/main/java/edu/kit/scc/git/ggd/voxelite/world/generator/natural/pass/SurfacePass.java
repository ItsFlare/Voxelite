package edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.NaturalWorldGenerator;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

public class SurfacePass implements GeneratorPassInstance<NaturalWorldGenerator> {
    private final NaturalWorldGenerator generator;

    public SurfacePass(NaturalWorldGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void apply(GeneratorChunk<NaturalWorldGenerator> chunk) {
        int aboveSea = (chunk.getWorldPosition().y() + Chunk.MAX_WIDTH) - TerrainPass.SEA_LEVEL;
        if (aboveSea <= 0) return;
        if (aboveSea > Chunk.WIDTH) aboveSea = Chunk.WIDTH;

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.WIDTH; z++) {
                final NaturalWorldGenerator.NoisePoint noisePoint = generator.sampleNoises(chunk.getWorldPosition().add(new Vec3f(x, 0, z))); //TODO Bad assumption
//                final float erosion = 0.5f * noisePoint.erosion() + 0.5f;
//                final int thickness = (int) ((1f - erosion) * 2f);
                final int thickness = 3;

                Voxel aboveNeighbor = chunk.getVoxel(new Vec3i(x, Chunk.WIDTH, z));
                if(aboveNeighbor == null) aboveNeighbor = Main.INSTANCE.getWorld().getVoxel(chunk.getWorldPosition().add(new Vec3i(x, Chunk.WIDTH, z))); //TODO Hacky

                int lastAir = aboveNeighbor.getBlock() == Block.AIR ? Chunk.WIDTH : Integer.MAX_VALUE;

                for (int y = Chunk.MAX_WIDTH; y >= Chunk.WIDTH - aboveSea - thickness; y--) {
                    var voxel = chunk.getVoxel(new Vec3i(x, y, z));
                    if(voxel == null) break; //Chunk was already loaded, too late :(

                    if (voxel.getBlock() == Block.AIR) lastAir = y;
                    else if (voxel.getBlock() == Block.STONE) {
                        boolean placeRiver = noisePoint.ridge() < -0.75f && noisePoint.erosion() > 0; //TODO Hardcoded
                        if (lastAir - y == 1) {
                            voxel.setBlock(placeRiver ? Block.CYAN_GLASS : Block.GRASS);
                        } else if (lastAir - y < thickness) {
                            voxel.setBlock(placeRiver ? Block.CYAN_GLASS : Block.DIRT);
                        }
                    }
                }
            }
        }
    }
}
