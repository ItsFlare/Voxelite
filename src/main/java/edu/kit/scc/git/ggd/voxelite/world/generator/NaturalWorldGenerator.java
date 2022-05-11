package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.World;
import edu.kit.scc.git.ggd.voxelite.world.generator.pass.GeneratorPass;
import edu.kit.scc.git.ggd.voxelite.world.generator.pass.TerrainPass;
import net.durchholz.beacon.math.Vec3i;

import java.util.List;

public class NaturalWorldGenerator implements WorldGenerator {

    private final List<GeneratorPass> passes;
    private World world;

    public NaturalWorldGenerator(long seed) {
        TerrainPass terrainPass = new TerrainPass(seed);
        this.passes = List.of(terrainPass);
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public Chunk generate(Vec3i position) {
        final Chunk chunk = new Chunk(world, position);

        for (GeneratorPass pass : passes) {
            pass.apply(chunk);
        }

        return chunk;
    }

    public long getSeed() {
        long seed = 0;
        return seed;
    }
}
