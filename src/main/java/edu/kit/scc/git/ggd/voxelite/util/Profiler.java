package edu.kit.scc.git.ggd.voxelite.util;

import java.util.stream.LongStream;

public class Profiler {
    private static final int BUFFER_SIZE_EXP = 7;
    private static final int BUFFER_SIZE = 1 << BUFFER_SIZE_EXP;
    private static final int MASK = BUFFER_SIZE - 1;

    private final long[] frameTime = new long[BUFFER_SIZE];

    private long frameStart;
    private int  tick;

    public void tick() {
        frameTime[tick++ & MASK] = System.nanoTime() - frameStart;
        frameStart = System.nanoTime();
    }

    public float frameTime() {
        return (float) (LongStream.of(frameTime).average().getAsDouble() / 1_000_000f);
    }

    public float maxFrameTime() {
        return LongStream.of(frameTime).max().getAsLong() / 1_000_000f;
    }

    public float minFrameTime() {
        return LongStream.of(frameTime).min().getAsLong() / 1_000_000f;
    }
}
