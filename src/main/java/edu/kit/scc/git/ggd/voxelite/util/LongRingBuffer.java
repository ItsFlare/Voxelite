package edu.kit.scc.git.ggd.voxelite.util;

import java.util.Arrays;
import java.util.stream.LongStream;

public abstract class LongRingBuffer {
    protected static final int BUFFER_SIZE_EXP = 8;
    protected static final int BUFFER_SIZE     = 1 << BUFFER_SIZE_EXP;
    protected static final int MASK            = BUFFER_SIZE - 1;

    public final long[] values = new long[BUFFER_SIZE];
    private int i = 0;

    public abstract void tick();

    public long get() {
        return values[(i - 1) & MASK];
    }

    public void set(long l) {
        values[i++ & MASK] = l;
    }

    public long average() {
        return (long) LongStream.of(values).average().getAsDouble();
    }

    public long max() {
        return LongStream.of(values).max().getAsLong();
    }

    public long min() {
        return LongStream.of(values).min().getAsLong();
    }

    public long jitter() {
        long avg = average();
        return (long) Arrays.stream(values).map(value -> value - avg).map(Math::abs).average().getAsDouble();
    }

    public float[] toArray(boolean shift) {
        float[] result = new float[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = values[(i + (shift ? this.i : 0)) & MASK];
        }

        return result;
    }
}
