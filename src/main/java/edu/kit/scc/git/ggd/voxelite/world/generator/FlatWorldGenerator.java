package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.World;
import edu.kit.scc.git.ggd.voxelite.world.generator.pass.GeneratorPass;
import net.durchholz.beacon.math.Vec3i;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FlatWorldGenerator implements WorldGenerator {
    private final Layer[] layers;
    private World world;

    public FlatWorldGenerator(Layer... layers) {
        this.layers = layers;
        Arrays.sort(layers, Comparator.comparingInt(Layer::height));
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public List<GeneratorPass> getPasses() {
        return null;
    }

    @Override
    public Chunk generate(Vec3i position) {
        Chunk chunk = new Chunk(world, position);
        for (Voxel voxel : chunk) {
            for (Layer layer : layers) {
                if(layer.height >= voxel.position().y()) {
                    voxel.setBlock(layer.block);
                    break;
                }
            }
        }

        return chunk;
    };

    public record Layer(Block block, int height) {}
}
