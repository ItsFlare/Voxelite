package edu.kit.scc.git.ggd.voxelite.ui;

public abstract class TitledElement implements Element {
    protected String title;

    protected TitledElement(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
