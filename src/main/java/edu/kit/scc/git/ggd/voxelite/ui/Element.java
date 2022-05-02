package edu.kit.scc.git.ggd.voxelite.ui;

import java.util.function.BiConsumer;

public abstract class Element<T> {

    private final BiConsumer<T, T> action;
    private T previous;

    protected Element(BiConsumer<T, T> action) {
        this.action = action;
    }

    protected abstract T read();
    public void draw() {
        tick();
    }

    private void tick() {
        T read = read();
        if(!read.equals(previous)) {
            action.accept(previous, read);
        }
        previous = read;
    }
}
