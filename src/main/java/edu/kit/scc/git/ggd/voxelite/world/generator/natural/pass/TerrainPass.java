package edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.NaturalWorldGenerator;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

public class TerrainPass implements GeneratorPassInstance<NaturalWorldGenerator> {
    private final NaturalWorldGenerator generator;

    public static final int SEA_LEVEL = 128;
    public static boolean onlyBaseHeight = false;

    public TerrainPass(NaturalWorldGenerator generator) {
        this.generator = generator;
    }

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

                for (int cy = isBottom ? 1 : 0; cy < Chunk.WIDTH; cy++) {
                    final Voxel voxel = chunk.getVoxel(new Vec3i(cx, cy, cz));
                    final Vec3i position = voxel.position();
                    final int y = position.y();

                    if(y == SEA_LEVEL) voxel.setBlock(Block.CYAN_GLASS);

                    //3D noise
                    int baseHeight = generator.getBaseHeight(noise);
                    float exposedHeight = 100;
                    float threshold = (y - baseHeight) / exposedHeight;
                    if(threshold < 0) threshold *= 2; //Steeper dropoff into negative
                    if(noise.ridge() < 0 && y > baseHeight) threshold += (-noise.ridge() * 0.5f); //Reduce 3D noise above valleys

                    float erosionNormalized = 0.5f * noise.erosion() + 0.5f;
                    float surfaceNoise = generator.getSurfaceNoise().sample(position) * (1 - erosionNormalized); //Use inverse of erosion as amplitude

                    if (onlyBaseHeight) {
                        if (y < baseHeight) voxel.setBlock(Block.STONE);
                    } else {
                        if (surfaceNoise > threshold) voxel.setBlock(Block.STONE);
                    }
                }
            }
        }
    }
}
