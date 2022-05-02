package edu.kit.scc.git.ggd.voxel.ui;

import imgui.ImGui;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FloatSliderElement extends Element<Float> {
    private final float[] value;
    private final String title;
    private final float min;
    private final float max;

    protected FloatSliderElement(String title, float initial, float min, float max, BiConsumer<Float, Float> action) {
        super(action);
        this.title = title;
        this.value = new float[]{initial};
        this.min = min;
        this.max = max;
    }

    protected FloatSliderElement(String title, float initial, float min, float max, Consumer<Float> action) {
        this(title, initial, min, max, (previous, next) -> action.accept(next));
    }

    @Override
    public void draw() {
        super.draw();
        ImGui.sliderFloat(title, value, min, max);
    }

    @Override
    protected Float read() {
        return value[0];
    }
}
