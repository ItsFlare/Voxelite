package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.render.RenderChunk;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import static edu.kit.scc.git.ggd.voxelite.world.Chunk.VOLUME;
import static edu.kit.scc.git.ggd.voxelite.world.Chunk.toLinearSpace;

@SuppressWarnings({"ConstantConditions", "PointlessArithmeticExpression"})
public class CompressedLightStorage implements LightStorage {

    static {
        //In its current implementation, this class will not work for different values
        assert CHANNELS == 2;
        assert RANGE_EXP == 5;
    }

    private static final int COMPONENTS = 3;
    private static final int BITS_PER_CHANNEL = COMPONENTS * RANGE_EXP;
    private static final int LSH = ((1 << BITS_PER_CHANNEL) - 1);
    private static final int MSH = LSH << BITS_PER_CHANNEL;
    private static final int COMPONENT_MASK = (1 << RANGE_EXP) - 1;

    private final int[] lights = new int[VOLUME];

    @Override
    public Vec3i getLight(Vec3i position) {
        final int linear = toLinearSpace(position);
        final int light = lights[linear];
        return decode(light);
    }

    public synchronized void calculate(Voxel voxel, Block previous) {
        final Block block = voxel.getBlock();
        final int range = max(get(voxel.position()));

        lights[toLinearSpace(voxel.position())] = block.getCompressedLight();

        //Store chunks to update
        final Set<Vec3i> chunks = new HashSet<>();

        //Store voxels to propagate light from
        final Queue<Voxel> propagationQueue = new LinkedList<>();

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
                        v.chunk().getLightStorage().lights[toLinearSpace(v.position())] = 0;
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

            final int light = v.chunk().getLightStorage().lights[toLinearSpace(v.position())];
            if (light == 0) continue;
            final int decremented = SIMD.decrement(light);

            for (Direction direction : Direction.values()) {
                final Voxel neighbor = v.getNeighbor(direction);
                if (neighbor == null) continue; //Neighbor not loaded
                if (neighbor.isOpaque()) continue;

                final var neighborStorage = neighbor.chunk().getLightStorage();
                final int neighborIndex = toLinearSpace(neighbor.position());
                final int neighborLight = neighborStorage.lights[neighborIndex];
                final int filter = neighbor.getBlock().getCompressedFilter();

                int neighborNewLight = SIMD.max(decremented, neighborLight) & filter;

                if (neighborLight != neighborNewLight) {
                    neighborStorage.lights[neighborIndex] = neighborNewLight;
                    propagationQueue.add(neighbor);
                    chunks.add(neighbor.chunk().getPosition());
                }
            }
        }

        //Update all affected chunks
        for (Vec3i chunkPosition : chunks) {
            final RenderChunk renderChunk = Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunk(chunkPosition);
            if (renderChunk != null) Main.INSTANCE.getRenderer().getWorldRenderer().queueRebuild(renderChunk); //TODO Make neater
        }
    }

    private int get(int linear) {
        return lights[linear];
    }

    private int get(Vec3i position) {
        return get(toLinearSpace(position));
    }

    public static int low(int light) {
        return light & LSH;
    }

    public static int high(int light) {
        return (light & MSH) >>> BITS_PER_CHANNEL;
    }

    public static int r(int light) {
        return b(light >>> (RANGE_EXP << 1));
    }

    public static int g(int light) {
        return b(light >>> RANGE_EXP);
    }

    public static int b(int light) {
        return light & COMPONENT_MASK;
    }

    public static int max(int light) {
        final int max = SIMD.max(light, light >> BITS_PER_CHANNEL);
        return Math.max(r(max), Math.max(g(max), b(max)));
    }

    public static int encode(int r, int g, int b) {
        assert r < RANGE && g < RANGE && b < RANGE;

        int result = 0;
        result |= b << (RANGE_EXP * 0);
        result |= g << (RANGE_EXP * 1);
        result |= r << (RANGE_EXP * 2);

        return result;
    }

    public static int encode(Vec3f color, int range) {
        if(range > LightStorage.RANGE || range < 0) throw new IllegalArgumentException();

        int channelsR = Math.round(color.x() * (float) LightStorage.CHANNELS);
        int channelsG = Math.round(color.y() * (float) LightStorage.CHANNELS);
        int channelsB = Math.round(color.z() * (float) LightStorage.CHANNELS);

        int result = 0; //TODO Optimize?
        result |= (channelsB > 0 ? range : 0) << (RANGE_EXP * 0);
        result |= (channelsG > 0 ? range : 0) << (RANGE_EXP * 1);
        result |= (channelsR > 0 ? range : 0) << (RANGE_EXP * 2);

        result |= (channelsB > 1 ? range : 0) << (RANGE_EXP * 3);
        result |= (channelsG > 1 ? range : 0) << (RANGE_EXP * 4);
        result |= (channelsR > 1 ? range : 0) << (RANGE_EXP * 5);

        return result;
    }

    public static Vec3i decode(int light) {
        final int l = low(light);
        final int h = high(light);

        final int r = r(l) + r(h);
        final int g = g(l) + g(h);
        final int b = b(l) + b(h);

        return new Vec3i(r, g, b);
    }

    public static final class SIMD {

        public static final int STRIPED_MASK = 0x1f07c1f; //Mask half of the components alternating
        public static final int CARRY_MASK   = 0x2008020; //MSB of masked components shifted left by one
        public static final int BORROW_GUARD = CARRY_MASK << 1; //Prevent underflow from propagating
        public static final int LSB_MASK     = 0x100401; //LSB of each component for decrement

        public static int lessThan(int a, int b) {
            return simdLessThanHalf(a, b) | (simdLessThanHalf(a >> RANGE_EXP, b >> RANGE_EXP) << RANGE_EXP);
        }

        public static int max(int a, int b) {
            return a ^ ((a ^ b) & lessThan(a, b));
        }

        public static int decrement(int x) {
            return simdDecrementHalf(x) | (simdDecrementHalf(x >> RANGE_EXP) << RANGE_EXP);
        }

        private static int simdLessThanHalf(int a, int b) {
            int d = (((a & STRIPED_MASK) | BORROW_GUARD) - (b & STRIPED_MASK)) & CARRY_MASK;
            int result = 0;
            for (int i = 1; i <= RANGE_EXP; i++) {
                result |= d >>> i;
            }
            return result;
        }

        private static int simdDecrementHalf(int x) {
            int result = (x & STRIPED_MASK);

            //Set underflow guard
            result |= BORROW_GUARD;

            //Decrement
            result -= LSB_MASK;

            //Check for underflow
            int uf = result & CARRY_MASK;

            //Increment underflowed values
            result += uf >> 5;

            result &= STRIPED_MASK;

            return result;
        }
    }

}
