package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.render.RenderChunk;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.Vec3i;

import java.util.*;
import java.util.stream.IntStream;

public class NaiveLightStorage implements LightStorage {
    private final Chunk   chunk;
    private final Vec3i[] lights = Collections.nCopies(Chunk.VOLUME << CHANNELS_EXP, new Vec3i()).toArray(Vec3i[]::new);

    public NaiveLightStorage(Chunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public Vec3i getLight(Vec3i position) {
        int linear = toLightLinearSpace(position);
        int r = 0, g = 0, b = 0;
        for (int i = 0; i < CHANNELS; i++) {
            r += lights[linear + i].x();
            g += lights[linear + i].y();
            b += lights[linear + i].z();
        }

        return new Vec3i(r, g, b);
    }

    @Override
    public void setLight(Vec3i position, Vec3i light, int range) {
        for (int i = 0; i < LightStorage.CHANNELS; i++) {
            lights[toLightLinearSpace(position) + i] = new Vec3i(light.x() > i ? range : 0, light.y() > i ? range : 0, light.z() > i ? range : 0);
        }
    }

    private static int toLightLinearSpace(Vec3i position) {
        return Chunk.toLinearSpace(position) << CHANNELS_EXP;
    }

    public void calculate(Voxel voxel, Block previous) {
        final Block block = voxel.getBlock();
        final int range = IntStream.range(0, CHANNELS).mapToObj(i -> lights[toLightLinearSpace(voxel.position()) + i]).mapToInt(Vec3i::max).max().getAsInt();

        setLight(voxel.position(), block.getLight(), LightStorage.RANGE); //TODO Variable range

        final Set<Vec3i> chunks = new HashSet<>();

        //Store voxels to propagate light from
        final Queue<Voxel> propagationQueue = new LinkedList<>();
        propagationQueue.add(voxel);

        //Propagate from neighbors if the new block is transparent
        if (block.isTransparent()) {
            for (Direction direction : Direction.values()) {
                final Voxel neighbor = voxel.getNeighbor(direction);
                if (neighbor != null) propagationQueue.add(neighbor);
            }
        }

        //Clear all lighting affected by this voxel, unless we set air and don't remove a light source or filter
        if (block != Block.AIR || previous.isLightSource() || previous.isTransparent()) {

            final Queue<Voxel> clearQueue = new LinkedList<>();
            final Set<Voxel> clearVisited = new HashSet<>();
            clearQueue.add(voxel);

            Voxel v;
            while ((v = clearQueue.poll()) != null) {
                if (!clearVisited.add(v)) continue;
                propagationQueue.add(v);

                final Vec3i delta = v.position().subtract(voxel.position());
                final int manhattan = Math.abs(delta.x()) + Math.abs(delta.y()) + Math.abs(delta.z());

                if (manhattan < range) {
                    if (!v.getBlock().isLightSource()) {

                        for (int c = 0; c < CHANNELS; c++) {
                            v.chunk().getLightStorage().lights[toLightLinearSpace(v.position()) + c] = new Vec3i();
                        }

                        chunks.add(v.chunk().getPosition());
                    }

                    for (Direction direction : Direction.values()) {
                        final Voxel neighbor = v.getNeighbor(direction);
                        if (neighbor == null) continue;
                        if (!clearVisited.contains(neighbor)) clearQueue.add(neighbor);
                    }
                }
            }
        }

        //Propagate light

        Voxel v;
        while ((v = propagationQueue.poll()) != null) {

            for (int c = 0; c < CHANNELS; c++) {
                final Vec3i light = v.chunk().getLightStorage().lights[toLightLinearSpace(v.position()) + c];
                if (light.equals(new Vec3i())) continue;

                for (Direction direction : Direction.values()) {
                    final Voxel neighbor = v.getNeighbor(direction);
                    if (neighbor == null) continue; //Neighbor not loaded
                    NaiveLightStorage neighborStorage = neighbor.chunk().getLightStorage();
                    if (neighbor.getBlock().isOpaque()) continue;

                    int neighborIndex = toLightLinearSpace(neighbor.position()) + c;
                    final Vec3i neighborLight = neighborStorage.lights[neighborIndex];
                    int r = Math.max(neighborLight.x(), light.x() - 1);
                    int g = Math.max(neighborLight.y(), light.y() - 1);
                    int b = Math.max(neighborLight.z(), light.z() - 1);

                    final Vec3i filter = neighbor.getBlock().getFilter();
                    final Vec3i neighborNewLight = new Vec3i(r * filter.x(), g * filter.y(), b * filter.z());

                    if (!neighborLight.equals(neighborNewLight)) {
                        neighborStorage.lights[neighborIndex] = neighborNewLight;
                        propagationQueue.add(neighbor);
                        chunks.add(neighbor.chunk().getPosition());
                    }
                }
            }
        }

        //Update all affected chunks
        for (Vec3i chunkPosition : chunks) {
            final RenderChunk renderChunk = Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunk(chunkPosition);
            if (renderChunk != null) Main.INSTANCE.getRenderer().getWorldRenderer().queueRebuild(renderChunk); //TODO Make neater
        }
    }
}
