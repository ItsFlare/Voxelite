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
                final Biome biome = Biome.select(noisePoint);

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
                        }
                    }
                }
            }
        }
    }
}
