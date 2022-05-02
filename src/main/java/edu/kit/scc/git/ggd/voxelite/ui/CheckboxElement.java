package edu.kit.scc.git.ggd.voxelite.ui;

import imgui.ImGui;
import imgui.type.ImBoolean;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CheckboxElement extends Element<Boolean> {
    private String title;
    private ImBoolean value;

    public CheckboxElement(String title, boolean initial, BiConsumer<Boolean, Boolean> action) {
        super(action);
        this.title = title;
        this.value = new ImBoolean(initial);
    }

    public CheckboxElement(String title, boolean initial, Consumer<Boolean> action) {
        this(title, initial, (previous, next) -> action.accept(next));
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
