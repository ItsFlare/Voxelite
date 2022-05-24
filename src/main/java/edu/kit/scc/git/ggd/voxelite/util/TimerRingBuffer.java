package edu.kit.scc.git.ggd.voxelite.util;

public class TimerRingBuffer extends LongRingBuffer {

    private long time;

    @Override
    public void tick() {
        set(System.nanoTime() - time);
        time = System.nanoTime();
    }
}
