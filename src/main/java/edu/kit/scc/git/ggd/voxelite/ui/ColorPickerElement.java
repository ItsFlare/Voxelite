package edu.kit.scc.git.ggd.voxelite.ui;

import imgui.ImGui;
import net.durchholz.beacon.math.Vec4f;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ColorPickerElement extends StateElement<Vec4f> {
    private final float[] value;

    public ColorPickerElement(String title, Vec4f initial, BiConsumer<Vec4f, Vec4f> action) {
        super(title, action);
        this.value = initial.array();
    }

    public ColorPickerElement(String title, Vec4f initial, Consumer<Vec4f> action) {
        this(title, initial, (previous, next) -> action.accept(next));
    }

    @Override
    public void draw() {
        super.draw();
        ImGui.colorEdit4(title, value);
    }

    @Override
    protected Vec4f read() {
        return new Vec4f(value[0], value[1], value[2], value[3]);
    }
}
