package edu.kit.scc.git.ggd.voxel.ui;

import edu.kit.scc.git.ggd.voxel.Main;
import edu.kit.scc.git.ggd.voxel.input.InputListener;
import edu.kit.scc.git.ggd.voxel.render.Camera;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.window.Window;

public class UserInterface {
    private static final boolean SAVE_GUI = true;

    private final Main          main;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3  imGuiGl3  = new ImGuiImplGl3();

    private final IntSliderElement fov;
    private final FloatSliderElement sensitivity, speed;
    private final CheckboxElement skybox, vsync, wireframe;

    public UserInterface(Main main) {
        this.main = main;

        this.fov = new IntSliderElement("FOV", Camera.DEFAULT_FOV, 5, 180, value -> main.getRenderer().getCamera().setFOV(value));
        this.sensitivity = new FloatSliderElement("Sensitivity", InputListener.DEFAULT_SENSITIVITY, 0.01f, 10, value -> main.getInputListener().sensitivity = value);
        this.speed = new FloatSliderElement("Speed", InputListener.DEFAULT_CAMERA_SPEED, 0.01f, 10, value -> main.getInputListener().cameraSpeed = value);

        this.skybox = new CheckboxElement("Skybox", true, value -> main.getRenderer().renderSkybox = value);
        this.vsync = new CheckboxElement("VSync", true, value ->Window.swapInterval(value ? 1 : 0));
        this.wireframe = new CheckboxElement("Wireframe", false, value -> main.getRenderer().wireframe = value);
    }

    public void init() {
        ImGui.createContext();
        if (!SAVE_GUI) ImGui.getIO().setIniFilename(null);
        imGuiGlfw.init(main.getWindow().id(), true);
        imGuiGl3.init("#version 410");
    }

    public void shutdown() {
        ImGui.destroyContext();
    }

    public void tick() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        ImGui.begin("Debug");

        drawProfiler(main.getProfiler().frameTime());
        drawPosition(main.getRenderer().getCamera().getPosition());
        drawSettings();

        ImGui.end();
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void drawProfiler(float frameTime) {
        ImGui.text("%d FPS (%.2fms)".formatted((int) (1_000 / frameTime), frameTime));
    }

    private void drawPosition(Vec3f position) {
        ImGui.text("Position: " + position.round());
    }

    private void drawSettings() {
        fov.draw();
        sensitivity.draw();
        speed.draw();

        skybox.draw();
        vsync.draw();
        wireframe.draw();
    }

}
