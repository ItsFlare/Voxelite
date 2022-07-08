package edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.NaturalWorldGenerator;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome.Biome;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.SimplexNoise;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

public class SurfacePass implements GeneratorPassInstance<NaturalWorldGenerator> {
    private static final Noise FEATURE_NOISE     = new SimplexNoise();
    private static final float FEATURE_THRESHOLD = 0.9f;

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
                final Biome biome = generator.selectBiome(noisePoint);

                Voxel aboveNeighbor = chunk.getVoxel(new Vec3i(x, Chunk.WIDTH, z));
                if (aboveNeighbor == null) aboveNeighbor = Main.INSTANCE.getWorld().getVoxel(chunk.getWorldPosition().add(new Vec3i(x, Chunk.WIDTH, z))); //TODO Hacky

                int lastAir = aboveNeighbor.getBlock() == Block.AIR ? Chunk.WIDTH : Integer.MAX_VALUE;

                for (int y = Chunk.MAX_WIDTH; y >= Chunk.WIDTH - aboveSea; y--) {
                    var voxel = chunk.getVoxel(new Vec3i(x, y, z));
                    if (voxel == null) {
                        voxel = Main.INSTANCE.getWorld().getVoxel(chunk.getWorldPosition().add(new Vec3i(x, y, z))); //TODO Hacky
                        if(voxel == null) continue;
                    }

                    if (voxel.getBlock() == Block.AIR) lastAir = y;
                    else if (voxel.getBlock() == Block.STONE) {
                        boolean placeRiver = noisePoint.ridge() < -0.75f && noisePoint.erosion() > 0; //TODO Place river

                        if (lastAir - y == 1) {
                            biome.getSurfaceLayer().place(voxel);

                            final float featureNoiseSample = FEATURE_NOISE.sample(chunk.getWorldPosition().xz().add(new Vec2f(x, z)).scale(biome.getFeatureDensity() * 10));
                            boolean placeFeature = featureNoiseSample > FEATURE_THRESHOLD;
                            if (placeFeature) {
                                var features = biome.getFeatures();
                                if (features.length != 0) {
                                    var feature = features[(int) (((featureNoiseSample - FEATURE_THRESHOLD) / (1f - FEATURE_THRESHOLD)) * (features.length - 1))];
                                    final Voxel neighbor = voxel.getNeighbor(Direction.POS_Y);
                                    if(neighbor != null) feature.place(neighbor);
                                }
                            }
                            Voxel neighbor = voxel.getNeighbor(Direction.POS_Y);
                            if(neighbor != null) {
                                float nois = new SimplexNoise().sample( voxel.getNeighbor(Direction.POS_Y).position());
                                if (nois > 0.80) {
                                    placeIgloo(voxel.getNeighbor(Direction.POS_Y));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private void placeIgloo(Voxel voxel) {
        Voxel relative;

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y < 7; y++) {
                    relative = voxel.getRelative(new Vec3i(x, y, z));
                    if (relative == null) {
                        return;
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
    }
    private boolean place(Voxel voxel, Biome biome) {
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
