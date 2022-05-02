package edu.kit.scc.git.ggd.voxel.ui;

import edu.kit.scc.git.ggd.voxel.Main;
import edu.kit.scc.git.ggd.voxel.input.InputListener;
import edu.kit.scc.git.ggd.voxel.render.Camera;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.window.Window;

public class UserInterface {
    private static final boolean SAVE_GUI = true;

    private final Main          main;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3  imGuiGl3  = new ImGuiImplGl3();

    public UserInterface(Main main) {
        this.main = main;
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
        ImGui.text("Position: " + position);
    }

    private final int[]     fov         = {Camera.DEFAULT_FOV};
    private final float[]   sensitivity = {InputListener.DEFAULT_SENSITIVITY};
    private final float[]   cameraSpeed = {InputListener.DEFAULT_CAMERA_SPEED};
    private final ImBoolean skybox      = new ImBoolean(true);
    private final ImBoolean vsync       = new ImBoolean(true);
    private final ImBoolean wireframe   = new ImBoolean(false);

    private void drawSettings() {
        ImGui.sliderInt("FOV", fov, 5, 180);
        main.getRenderer().getCamera().setFOV(fov[0]);

        ImGui.sliderFloat("Sensitivity", sensitivity, 0.01f, 10);
        main.getInputListener().sensitivity = sensitivity[0];

        ImGui.sliderFloat("Speed", cameraSpeed, 0.01f, 10);
        main.getInputListener().cameraSpeed = cameraSpeed[0];

        ImGui.checkbox("Skybox", skybox);
        main.getRenderer().renderSkybox = skybox.get();

        ImGui.checkbox("VSync", vsync);
        Window.swapInterval(vsync.get() ? 1 : 0);

        ImGui.checkbox("Wireframe", wireframe);
        main.getRenderer().wireframe = wireframe.get();
    }

}
