package edu.kit.scc.git.ggd.voxel.ui;

import edu.kit.scc.git.ggd.voxel.Main;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.durchholz.beacon.window.Window;

public class UserInterface {

    private final Window window;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3  imGuiGl3  = new ImGuiImplGl3();

    public UserInterface(Window window) {
        this.window = window;
    }

    public void init() {
        ImGui.createContext();
        imGuiGlfw.init(window.id(), true);
        imGuiGl3.init("#version 410");
    }

    public void shutdown() {
        ImGui.destroyContext();
    }

    public void tick() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        ImGui.begin("Debug");

        drawFramerate(Main.PROFILER.frameTime());

        ImGui.end();
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void drawFramerate(float frameTime) {
        ImGui.text("%d FPS (%.2fms)".formatted((int) (1_000 / frameTime), frameTime));
    }
}
