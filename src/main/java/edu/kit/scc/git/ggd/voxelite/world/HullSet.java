package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.util.Util;

import java.util.BitSet;
import java.util.Set;

/**
 * Stores visibility between hull faces of a chunk (inherently symmetrical)
 */
public class HullSet {
    private static final Direction MAX_DIRECTION      = Direction.values()[Direction.values().length - 1];
    private static final int       BITS_PER_DIRECTION = Util.log2(Direction.values().length);
    private static final int       BIT_COUNT          = toLinearSpace(MAX_DIRECTION, MAX_DIRECTION);

    private final BitSet bits = new BitSet(BIT_COUNT);

    public boolean contains(Direction a, Direction b) {
        return this.bits.get(toLinearSpace(a, b));
    }

    public void add(Set<Direction> directions) {
        for (Direction a : directions) {
            for (Direction b : directions) {
                this.add(a, b);
            }
        }
    }

    public void add(Direction a, Direction b) {
        this.bits.set(toLinearSpace(a, b), true);
        this.bits.set(toLinearSpace(b, a), true);
    }

    public void addAll(boolean value) {
        this.bits.set(0, this.bits.size(), value);
    }

    private static int toLinearSpace(Direction a, Direction b) {
        return a.ordinal() | (b.ordinal() << BITS_PER_DIRECTION);
    }
}
