package edu.kit.scc.git.ggd.voxelite.util;

import java.util.function.LongSupplier;

public class SuppliedLongRingBuffer extends LongRingBuffer {
    private final LongSupplier supplier;

    public SuppliedLongRingBuffer(LongSupplier supplier) {
        this.supplier = supplier;
    }

    @Override
    public void tick() {
        set(supplier.getAsLong());
    }
}
