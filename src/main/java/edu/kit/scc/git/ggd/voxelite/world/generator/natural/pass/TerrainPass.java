package edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.NaturalWorldGenerator;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome.Biome;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.SimplexNoise;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

public class TerrainPass implements GeneratorPassInstance<NaturalWorldGenerator> {
    private final NaturalWorldGenerator generator;

    public static final int SEA_LEVEL = 128;
    public static boolean onlyBaseHeight = false;

    public TerrainPass(NaturalWorldGenerator generator) {
        this.generator = generator;
    }

    Noise noiseFunction = new SimplexNoise(12354);

    @Override
    public void apply(GeneratorChunk<NaturalWorldGenerator> chunk) {
        if(chunk.getPosition().y() < 0) return;
        final boolean isBottom = chunk.getPosition().y() == 0;

        if(isBottom) {
            for (int x = 0; x < Chunk.WIDTH; x++) {
                for (int z = 0; z < Chunk.WIDTH; z++) {
                    chunk.setBlock(new Vec3i(x, 0, z), Block.BEDROCK);
                }
            }
        }

        for (int cx = 0; cx < Chunk.WIDTH; cx++) {
            for (int cz = 0; cz < Chunk.WIDTH; cz++) {
                final NaturalWorldGenerator.NoisePoint noise = generator.sampleNoises(chunk.getWorldPosition().add(new Vec3f(cx, 0, cz)));
                final Biome biome = generator.selectBiome(noise);

                for (int cy = isBottom ? 1 : 0; cy < Chunk.WIDTH; cy++) {
                    final Voxel voxel = chunk.getVoxel(new Vec3i(cx, cy, cz));
                    final Vec3i position = voxel.position();
                    final int y = position.y();

                    if(y == SEA_LEVEL) {
                        if (noiseFunction.sample(position) > 0.9 && noise.temperature() < -0.5f) {
                            placeIceBerg(voxel);
                        }
                        if (voxel.getBlock() != Block.ICE) {
                            voxel.setBlock(Block.WATER);
                        }
                    }



                    //3D noise
                    int baseHeight = generator.getBaseHeight(noise);
                    float exposedHeight = 100;
                    float threshold = (y - baseHeight) / exposedHeight;
                    if(threshold < 0) threshold *= 2; //Steeper dropoff into negative
                    if(noise.ridge() < 0 && y > baseHeight) threshold += (-noise.ridge() * 0.5f); //Reduce 3D noise above valleys

                    float erosionNormalized = 0.5f * noise.erosion() + 0.5f;
                    float surfaceNoise = generator.getSurfaceNoise().sample(position) * (1 - erosionNormalized); //Use inverse of erosion as amplitude


                    if (onlyBaseHeight) {
                        if (y < baseHeight) {
                            if (voxel.position().y() < 50 && noiseFunction.sample(position) > 0.8) {
                                voxel.setBlock(Block.COPPER_ORE);
                            } else {
                                voxel.setBlock(Block.STONE);
                            }

                        }
                    } else {
                        if (surfaceNoise > threshold) {
                            if (voxel.position().y() < 50 && noiseFunction.sample(position) > 0.8) {
                                voxel.setBlock(Block.COPPER_ORE);
                            } else {
                                voxel.setBlock(Block.STONE);
                            }
                        }
                    }
                }
            }
        }
    }

    private void placeIceBerg(Voxel voxel) {
        Voxel relative1;
        Voxel relative2;
        Voxel relative3;
        Voxel relative4;


        for (int y = 0; y < 3; y++) {
            for (int x = -2 + y; x <= 2 - y; x++) {
                for (int z = -2 + y; z <= 2 - y; z++) {

                    if (y == 0) {
                        relative1 = voxel.getRelative(new Vec3i(x, y, z));
                        if(relative1 != null) {
                            //System.out.println("x:" + x + " y:" + y + " z:" + z);
                            relative1.setBlock(Block.ICE);
                        }
                        relative2 = voxel.getRelative(new Vec3i(x, y - 1, z));
                        if(relative2 != null) {
                            relative2.setBlock(Block.ICE);
                        }
                    } else if (y == 1) {
                        relative1 = voxel.getRelative(new Vec3i(x, y, z));
                        if(relative1 != null) {
                            relative1.setBlock(Block.ICE);
                        }
                        relative2 = voxel.getRelative(new Vec3i(x, y - 3, z));
                        if(relative2 != null) {
                            relative2.setBlock(Block.ICE);
                        }
                        relative3 = voxel.getRelative(new Vec3i(x, y + 1, z));
                        if(relative3 != null) {
                            relative3.setBlock(Block.ICE);
                        }
                        relative4 = voxel.getRelative(new Vec3i(x, y - 4, z));
                        if(relative4 != null) {
                            relative4.setBlock(Block.ICE);
                        }
                    } else {
                        relative1 = voxel.getRelative(new Vec3i(x, y + 1, z));
                        if(relative1 != null) {
                            relative1.setBlock(Block.ICE);
                        }
                        relative2 = voxel.getRelative(new Vec3i(x, y - 6, z));
                        if(relative2 != null) {
                            relative2.setBlock(Block.ICE);
                        }
                    }

                }
            }
        }
    }
}
