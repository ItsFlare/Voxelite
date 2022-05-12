package edu.kit.scc.git.ggd.voxelite.ui;

import java.util.function.BiConsumer;

public abstract class TitledElement<T> extends Element<T> {
    protected String title;

    protected TitledElement(String title, BiConsumer<T, T> action) {
        super(action);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
