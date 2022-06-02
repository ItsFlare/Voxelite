package edu.kit.scc.git.ggd.voxelite.util;

public record Directional<T>(T... values) {
    public Directional {
        if(values.length != Direction.values().length) throw new IllegalArgumentException();
    }

    public T get(Direction direction) {
        return values[direction.ordinal()];
    }

    public void set(Direction direction, T value) {
        values[direction.ordinal()] = value;
    }
}
