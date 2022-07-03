package edu.kit.scc.git.ggd.voxelite.world;

import java.util.Arrays;

public class NaiveChunkStorage<T> implements ChunkStorage<T> {
    private final T[] values;

    public NaiveChunkStorage() {
        this.values = (T[]) new Object[Chunk.VOLUME];
    }

    public NaiveChunkStorage(T fill) {
        this();
        Arrays.fill(values, fill);
    }

    public NaiveChunkStorage(T[] values) {
        assert values.length == Chunk.VOLUME;
        this.values = values;
    }

    @Override
    public T get(int linear) {
        return values[linear];
    }

    @Override
    public void set(int linear, T value) {
        values[linear] = value;
    }
}
