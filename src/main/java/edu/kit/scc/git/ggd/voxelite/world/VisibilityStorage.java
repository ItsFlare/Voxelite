package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import net.durchholz.beacon.math.Vec3i;
import org.jetbrains.annotations.Contract;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

/**
 * Stores an opacity bit per voxel and calculates visibility via flood-fill.
 */
public class VisibilityStorage {

    private static final int[] HULL = Util.init(int[]::new, () -> Chunk.VOLUME - (int) Math.pow(Chunk.WIDTH - 2, 3), array -> {
        int i = 0;

        for (Vec3i v : WorldChunk.iterate()) {
            if (v.min() == 0 || v.max() == Chunk.MAX_WIDTH) array[i++] = Chunk.toLinearSpace(v);
        }

        assert i == array.length;
    });

    private final BitSet opaque;

    private int opaqueCount;

    public VisibilityStorage() {
        opaque = new BitSet(Chunk.VOLUME);
    }

    private VisibilityStorage(VisibilityStorage storage) {
        this.opaque = (BitSet) storage.opaque.clone();
        this.opaqueCount = storage.opaqueCount;
    }

    public boolean get(Vec3i position) {
        return get(Chunk.toLinearSpace(position));
    }

    public boolean get(int linear) {
        return this.opaque.get(linear);
    }

    public void set(Vec3i position, boolean opaque) {
        set(Chunk.toLinearSpace(position), opaque);
    }

    public void set(int linear, boolean opaque) {
        if (!get(linear) && opaque) opaqueCount++;
        else if (get(linear) && !opaque) opaqueCount--;

        this.opaque.set(linear, opaque);
    }

    /**
     * Calculate a hull set based on a copy of the current visibility state.
     * @return New populated hull set
     */
    @Contract(pure = true)
    public HullSet calculate() {
        final HullSet hullSet = new HullSet();
        final VisibilityStorage copy = copy();

        if (opaqueCount < Chunk.AREA) {
            //Too few blocks to cover one side
            hullSet.addAll(true);
        } else if (copy.opaqueCount != Chunk.VOLUME) {
            for (int linear : HULL) {
                if (!copy.get(linear)) {
                    hullSet.add(copy.floodFill(linear));
                }
            }
        }

        return hullSet;
    }

    public Set<Direction> floodFill(Vec3i position) {
        return this.floodFill(Chunk.toLinearSpace(position));
    }

    /**
     * Flood-fill and collect reached hull sides.
     * Visited voxels are set opaque (impure operation).
     * @param linear Flood-fill origin
     * @return Set of reached hull sides
     */
    @Contract(pure = false)
    public Set<Direction> floodFill(int linear) {
        final EnumSet<Direction> set = EnumSet.noneOf(Direction.class);
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

        queue.enqueue(linear);
        set(linear, true);

        while (!queue.isEmpty()) {
            int current = queue.dequeueInt();

            testHull(current, set);

            for (Direction direction : Direction.values()) {
                int neighborLinear = getNeighborIndex(current, direction);
                boolean outside = neighborLinear < 0;

                if (!outside && !get(neighborLinear)) {
                    set(neighborLinear, true);
                    queue.enqueue(neighborLinear);
                }
            }
        }

        return set;
    }

    public VisibilityStorage copy() {
        return new VisibilityStorage(this);
    }

    /**
     * Tests whether a voxel position is part of any hull face.
     * @param linear Voxel position to check
     * @param set Set of directions to add matched hull faces to
     */
    private static void testHull(int linear, Set<Direction> set) {
        final Vec3i position = Chunk.fromLinearSpace(linear);

        if (position.x() == 0) {
            set.add(Direction.NEG_X);
        } else if (position.x() == Chunk.MAX_WIDTH) {
            set.add(Direction.POS_X);
        }

        if (position.y() == 0) {
            set.add(Direction.NEG_Y);
        } else if (position.y() == Chunk.MAX_WIDTH) {
            set.add(Direction.POS_Y);
        }

        if (position.z() == 0) {
            set.add(Direction.NEG_Z);
        } else if (position.z() == Chunk.MAX_WIDTH) {
            set.add(Direction.POS_Z);
        }
    }

    /**
     * Calculate index for a neighboring voxel position.
     * @param linear Origin position
     * @param direction Neighbor direction
     * @return Neighbor position if contained in chunk, {@literal -1} otherwise
     */
    private static int getNeighborIndex(int linear, Direction direction) {
        return switch (direction) {
            case POS_X -> Chunk.x(linear) == Chunk.MAX_WIDTH ? -1 : (linear + Chunk.AREA);
            case POS_Y -> Chunk.y(linear) == Chunk.MAX_WIDTH ? -1 : (linear + 1);
            case POS_Z -> Chunk.z(linear) == Chunk.MAX_WIDTH ? -1 : (linear + Chunk.WIDTH);
            case NEG_X -> Chunk.x(linear) == 0 ? -1 : (linear - Chunk.AREA);
            case NEG_Y -> Chunk.y(linear) == 0 ? -1 : (linear - 1);
            case NEG_Z -> Chunk.z(linear) == 0 ? -1 : (linear - Chunk.WIDTH);
        };
    }
}
