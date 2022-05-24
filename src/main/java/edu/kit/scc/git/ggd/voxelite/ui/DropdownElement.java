package edu.kit.scc.git.ggd.voxelite.ui;

import imgui.ImGui;
import imgui.type.ImInt;

import java.util.Map;
import java.util.function.BiConsumer;

public class DropdownElement<T> extends StateElement<T> {

    protected final Map<String, T> values;
    protected final String[]       items;
    protected       ImInt          current = new ImInt();

    protected DropdownElement(String title, Map<String, T> values) {
        this(title, values, (t, t2) -> {});
    }
    protected DropdownElement(String title, Map<String, T> values, BiConsumer<T, T> action) {
        super(title, action);
        this.values = values;
        items = values.keySet().toArray(new String[0]);
    }

    @Override
    public void draw() {
        super.draw();
        ImGui.listBox(title, current, items);
    }

    @Override
    protected T read() {
        return values.get(items[current.get()]);
    }
}
