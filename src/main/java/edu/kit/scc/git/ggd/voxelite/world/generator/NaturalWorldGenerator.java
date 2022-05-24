package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.World;
import edu.kit.scc.git.ggd.voxelite.world.generator.pass.CavePass;
import edu.kit.scc.git.ggd.voxelite.world.generator.pass.GeneratorPass;
import edu.kit.scc.git.ggd.voxelite.world.generator.pass.TerrainPass;
import net.durchholz.beacon.math.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NaturalWorldGenerator implements WorldGenerator {

    private final List<GeneratorPass> passes;
    private World world;

    public NaturalWorldGenerator(long seed) {
        passes = new ArrayList<>();
        TerrainPass terrainPass = new TerrainPass(seed);
        CavePass cavePass = new CavePass(seed);
        passes.add(terrainPass);
        passes.add(cavePass);
        //this.passes = List.of(terrainPass);
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public List<GeneratorPass> getPasses() {
        return passes;
    }

    @Override
    public Chunk generate(Vec3i position) {
        Chunk chunk = new Chunk(world, position);

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
