package edu.kit.scc.git.ggd.voxelite.ui;

import imgui.ImGui;

import java.util.function.Supplier;

public class PlotElement extends TitledElement {
    private final Type              type;
    private final int               height;
    private final Supplier<float[]> data;
    private final Supplier<String> overlayText;
    private final TextElement      titleElement;

    protected PlotElement(String title, Type type, int height, Supplier<float[]> data, Supplier<String> overlayText) {
        super(title);
        this.type = type;
        this.height = height;
        this.data = data;
        this.overlayText = overlayText;
        this.titleElement = new TextElement(() -> this.title, true);
    }

    @Override
    public void draw() {
        final float[] data = this.data.get();
        float max = 0;
        for (float f : data) {
            if (f > max) max = f;
        }

        titleElement.draw();

        switch (type) {
            case HISTOGRAM -> ImGui.plotHistogram(title, data, data.length, 0, overlayText.get(), 0, max, ImGui.getColumnWidth(), height);
            case LINES -> ImGui.plotLines(title, data, data.length, 0, overlayText.get(), 0, max, ImGui.getColumnWidth(), height);
        }
    }

    public enum Type {
        HISTOGRAM, LINES;
    }
}
