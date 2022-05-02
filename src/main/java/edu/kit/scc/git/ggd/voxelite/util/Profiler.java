package edu.kit.scc.git.ggd.voxelite.util;

import java.util.stream.LongStream;

public class Profiler {
    private static final int BUFFER_SIZE = 100;

    private final long[] frameTime = new long[BUFFER_SIZE];

    private long frameStart;
    private int  tick;

    public void tick() {
        frameTime[tick++ % 100] = System.nanoTime() - frameStart;
        frameStart = System.nanoTime();
    }

    public float frameTime() {
        return (float) (LongStream.of(frameTime).average().getAsDouble() / 1_000_000f);
    }
}
