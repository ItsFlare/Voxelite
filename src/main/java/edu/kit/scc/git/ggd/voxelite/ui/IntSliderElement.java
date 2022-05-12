package edu.kit.scc.git.ggd.voxelite.ui;

import imgui.ImGui;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class IntSliderElement extends TitledElement<Integer> {
    private final int[] value;
    private final int min;
    private final int max;

    public IntSliderElement(String title, int initial, int min, int max, BiConsumer<Integer, Integer> action) {
        super(title, action);
        this.value = new int[]{initial};
        this.min = min;
        this.max = max;
    }

    public IntSliderElement(String title, int initial, int min, int max, Consumer<Integer> action) {
        this(title, initial, min, max, (previous, next) -> action.accept(next));
    }

    @Override
    public void draw() {
        super.draw();
        ImGui.sliderInt(title, value, min, max);
    }

    @Override
    protected Integer read() {
        return value[0];
    }
}
