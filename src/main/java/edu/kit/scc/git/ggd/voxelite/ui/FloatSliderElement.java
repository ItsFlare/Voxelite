package edu.kit.scc.git.ggd.voxelite.ui;

import imgui.ImGui;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FloatSliderElement extends TitledElement<Float> {
    private final float[] value;
    private final float min;
    private final float max;

    public FloatSliderElement(String title, float initial, float min, float max, BiConsumer<Float, Float> action) {
        super(title, action);
        this.value = new float[]{initial};
        this.min = min;
        this.max = max;
    }

    public FloatSliderElement(String title, float initial, float min, float max, Consumer<Float> action) {
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
