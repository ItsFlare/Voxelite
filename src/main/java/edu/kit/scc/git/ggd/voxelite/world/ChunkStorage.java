package edu.kit.scc.git.ggd.voxelite.world;

public interface ChunkStorage<T> {
    T get(int linear);
    void set(int linear, T value);
}
