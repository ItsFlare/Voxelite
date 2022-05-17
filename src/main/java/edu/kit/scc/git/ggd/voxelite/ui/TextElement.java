package edu.kit.scc.git.ggd.voxelite.ui;

import imgui.ImGui;
import imgui.ImVec2;

import java.util.function.Supplier;

public class TextElement implements Element {
    private final Supplier<String> supplier;
    private final boolean          center;

    public TextElement(Supplier<String> supplier) {
        this(supplier, false);
    }

    public TextElement(Supplier<String> supplier, boolean center) {
        this.supplier = supplier;
        this.center = center;
    }

    @Override
    public void draw() {
        String text = supplier.get();
        if(center) {
            final ImVec2 imVec2 = new ImVec2();
            ImGui.calcTextSize(imVec2, text);
            ImGui.setCursorPosX((ImGui.getWindowSizeX() - imVec2.x) * 0.5f);
        }
        ImGui.text(text);
    }
}
