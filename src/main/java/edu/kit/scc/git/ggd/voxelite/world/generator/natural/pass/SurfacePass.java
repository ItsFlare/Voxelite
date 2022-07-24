package edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.HeightMap;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.NaturalWorldGenerator;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome.Biome;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome.Structure;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.Random;
import java.util.function.Predicate;

public class SurfacePass implements GeneratorPassInstance<NaturalWorldGenerator> {
    private static final int CELL_SIZE = 32;
    public static final float CELL_COVERAGE = CELL_SIZE / (float) Chunk.WIDTH;
    public static final int CELL_COUNT = (Chunk.WIDTH / CELL_SIZE);

    private final NaturalWorldGenerator generator;

    public SurfacePass(NaturalWorldGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void apply(GeneratorChunk<NaturalWorldGenerator> chunk) {
        HeightMap heightMap = chunk.getHeightMap();

        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.WIDTH; z++) {
                Voxel voxel = getSurface(chunk, x , z, b -> b == Block.STONE);
                if (voxel != null) {
                    heightMap.set(x, z, voxel);
                }
            }
        }


        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.WIDTH; z++) {
                final NaturalWorldGenerator.NoisePoint noisePoint = generator.sampleNoises(chunk.getWorldPosition().add(new Vec3f(x, 0, z))); //TODO Bad assumption
                final Biome biome = generator.selectBiome(noisePoint);
                Voxel voxel = getSurfaceVoxel(chunk, x, z);
                if (voxel != null) {
                    biome.getSurfaceLayer().place(voxel);
                }
            }
        }
        placeStructures(chunk);
    }

    private Voxel getSurface(Chunk chunk, int x, int z, Predicate<Block> filter) {
        int aboveSea = (chunk.getWorldPosition().y() + Chunk.MAX_WIDTH) - TerrainPass.SEA_LEVEL;
        if (aboveSea <= 0) return null;
        if (aboveSea > Chunk.WIDTH) aboveSea = Chunk.WIDTH;

        Voxel aboveNeighbor = chunk.getVoxel(new Vec3i(x, Chunk.WIDTH, z));
        if (aboveNeighbor == null) aboveNeighbor = Main.INSTANCE.getWorld().getVoxel(chunk.getWorldPosition().add(new Vec3i(x, Chunk.WIDTH, z))); //TODO Hacky

        int lastAir = aboveNeighbor.getBlock() == Block.AIR ? Chunk.WIDTH : Integer.MAX_VALUE;


        for (int y = Chunk.MAX_WIDTH; y >= Chunk.WIDTH - aboveSea; y--) {
            var voxel = chunk.getVoxel(new Vec3i(x, y, z));
            if (voxel == null) {
                voxel = Main.INSTANCE.getWorld().getVoxel(chunk.getWorldPosition().add(new Vec3i(x, y, z))); //TODO Hacky
                if (voxel == null) continue;
            }

            if (voxel.getBlock() == Block.AIR) lastAir = y;
            else if (filter.test(voxel.getBlock())) {
                if (lastAir - y == 1) {
                    return voxel.getNeighbor(Direction.POS_Y);
                }
            }
        }
        return null;
    }

    public void placeStructures(GeneratorChunk<NaturalWorldGenerator> chunk) {
        final Random random = new Random(((generator.getSeed() ^ chunk.getPosition().x()) + chunk.getPosition().y()) * chunk.getPosition().z());


        for (int cx = 0; cx < CELL_COUNT; cx++) {
            for (int cz = 0; cz < CELL_COUNT; cz++) {
                Vec3i cell = new Vec3i(cx, 0 , cz).scale(CELL_SIZE);
                Biome biome = generator.selectBiome(generator.sampleNoises(chunk.getWorldPosition().add(new Vec3f(cx, 0, cz).scale(Chunk.WIDTH / CELL_SIZE).add(CELL_SIZE / 2))));

                for (Structure structure : biome.getStructures()) {
                    int minAmount = structure.minStruct();
                    int maxAmount = structure.maxStruct();

                    int amount = (int) ((minAmount + random.nextInt(maxAmount - minAmount)) * CELL_COVERAGE);

                    for (int i = 0; i < amount; i++) {
                        int x = (cell.x() + random.nextInt(CELL_SIZE));
                        int z = (cell.z() + random.nextInt(CELL_SIZE));

                        Voxel voxel = getSurfaceVoxel(chunk, x, z);
                        if(voxel != null) {
                            structure.feature().place(voxel);
                        }

                    }
                }
            }
        }
    }

    private Voxel getSurfaceVoxel(GeneratorChunk<NaturalWorldGenerator> chunk, int x, int z) {
        return chunk.getHeightMap().get(x, z);
    }

}
