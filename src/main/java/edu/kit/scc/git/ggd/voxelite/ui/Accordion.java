package edu.kit.scc.git.ggd.voxelite.ui;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;

public class Accordion extends TitledElement {
    private final boolean   opened;
    private final Element[] elements;

    public Accordion(String title, boolean opened, Element... elements) {
        super(title);
        this.opened = opened;
        this.elements = elements;
    }

    public void draw() {
        int flags = opened ? ImGuiTreeNodeFlags.DefaultOpen : ImGuiTreeNodeFlags.None;
        if (ImGui.collapsingHeader(title, flags)) {
            for (Element element : elements) {
                element.draw();
            }
        }
    }
}
