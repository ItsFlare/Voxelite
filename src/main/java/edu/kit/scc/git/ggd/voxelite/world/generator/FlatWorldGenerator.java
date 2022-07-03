package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.World;
import edu.kit.scc.git.ggd.voxelite.world.WorldChunk;
import net.durchholz.beacon.math.Vec3i;

import java.util.Arrays;
import java.util.Comparator;

public class FlatWorldGenerator implements WorldGenerator {
    private final Layer[] layers;
    private World world;

    public FlatWorldGenerator(Layer... layers) {
        this.layers = layers;
        Arrays.sort(layers, Comparator.comparingInt(Layer::height));
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    public WorldChunk generate(Vec3i position) {
        WorldChunk chunk = new WorldChunk(world, position);
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
