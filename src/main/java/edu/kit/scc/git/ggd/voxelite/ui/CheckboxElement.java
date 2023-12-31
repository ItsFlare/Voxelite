package edu.kit.scc.git.ggd.voxelite.ui;

import imgui.ImGui;
import imgui.type.ImBoolean;

import java.util.function.Consumer;

public class CheckboxElement extends StateElement<Boolean> {
    private final ImBoolean value;

    public CheckboxElement(String title, boolean initial, Consumer<Boolean> action) {
        super(title, (previous, next) -> action.accept(next));
        this.value = new ImBoolean(initial);
    }

    @Override
    public void draw() {
        super.draw();
        ImGui.checkbox(title, value);
    }

    @Override
    protected Boolean read() {
        return value.get();
    }
}
