package edu.kit.scc.git.ggd.voxelite.ui;

import java.util.function.BiConsumer;

public abstract class StateElement<T> extends TitledElement {

    private final BiConsumer<T, T> action;
    private T previous;

    protected StateElement(String title, BiConsumer<T, T> action) {
        super(title);
        this.action = action;
    }

    protected abstract T read();
    @Override
    public void draw() {
        tick();
    }

    private void tick() {
        T read = read();
        if(read != previous) {
            action.accept(previous, read);
        }
        previous = read;
    }
}
