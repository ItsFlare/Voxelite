package edu.kit.scc.git.ggd.voxelite.ui;

import imgui.ImGui;

public class ButtonElement extends TitledElement {
    private final Runnable action;

    protected ButtonElement(String title, Runnable action) {
        super(title);
        this.action = action;
    }

    @Override
    public void draw() {
        if(ImGui.button(title)) {
            action.run();
        }
    }
}
