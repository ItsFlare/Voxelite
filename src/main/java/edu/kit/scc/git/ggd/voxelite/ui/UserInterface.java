package edu.kit.scc.git.ggd.voxelite.ui;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.input.InputListener;
import edu.kit.scc.git.ggd.voxelite.render.Camera;
import edu.kit.scc.git.ggd.voxelite.render.ChunkProgram;
import edu.kit.scc.git.ggd.voxelite.render.RenderChunk;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.ModuloChunkGenerator;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.window.Window;

public class UserInterface {
    private static final boolean SAVE_GUI = true;

    private final Main          main;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3  imGuiGl3  = new ImGuiImplGl3();

    private final IntSliderElement fov, chunkRadius, blockCount;
    private final FloatSliderElement sensitivity, speed;
    private final CheckboxElement skybox, world, vsync, wireframe, directionCulling, backfaceCulling;

    public UserInterface(Main main) {
        this.main = main;

        this.fov = new IntSliderElement("FOV", Camera.DEFAULT_FOV, 5, 180, value -> main.getRenderer().getCamera().setFOV(value));
        this.sensitivity = new FloatSliderElement("Sensitivity", InputListener.DEFAULT_SENSITIVITY, 0.01f, 10, value -> main.getInputListener().sensitivity = value);
        this.speed = new FloatSliderElement("Speed", InputListener.DEFAULT_CAMERA_SPEED, 0.01f, 10, value -> main.getInputListener().cameraSpeed = value);

        this.skybox = new CheckboxElement("Skybox", true, value -> main.getRenderer().renderSkybox = value);
        this.world = new CheckboxElement("World", true, value -> main.getRenderer().renderWorld = value);
        this.vsync = new CheckboxElement("VSync", true, value -> Window.swapInterval(value ? 1 : 0));
        this.wireframe = new CheckboxElement("Wireframe", false, value -> main.getRenderer().wireframe = value);
        this.directionCulling = new CheckboxElement("Direction Culling", true, value -> ChunkProgram.directionCulling = value);
        this.backfaceCulling = new CheckboxElement("Backface Culling", true, OpenGL::cull);

        this.chunkRadius = new IntSliderElement("Chunk radius", 1, 1, 50, main.getWorld()::setChunkRadius);

        this.blockCount = new IntSliderElement("Block gen modulo", 2, 1, 50, value -> {
            if(main.getWorld().getGenerator() instanceof ModuloChunkGenerator r) {
                r.modulo = value;

                main.getWorld().getChunks().stream().map(Chunk::getPosition).toList().forEach(vec3i -> {
                    main.getWorld().unloadChunk(vec3i);
                    main.getWorld().loadChunk(vec3i);
                });

                main.getRenderer().getWorldRenderer().updateMeshes();
            }
        });
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

        drawProfiler();
        drawPosition();
        drawSettings();

        ImGui.end();
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void drawProfiler() {
        var frameTime = main.getProfiler().frameTime();
        ImGui.text("%d FPS (AVG %.2fms | MIN %.2fms | MAX  %.2fms)".formatted((int) (1_000 / frameTime), frameTime, main.getProfiler().minFrameTime(), main.getProfiler().maxFrameTime()));
        ImGui.text("Chunks: %d | Blocks: %d | Quads: %d"
                .formatted(
                        main.getWorld().getChunks().size(),
                        main.getWorld().getChunks().stream().mapToInt(Chunk::getBlockCount).sum(),
                        main.getRenderer().getWorldRenderer().getRenderChunks().stream().mapToInt(RenderChunk::getQuadCount).sum()
                ));
    }

    private void drawPosition() {
        ImGui.text("Position: " + main.getRenderer().getCamera().getPosition().round());
    }

    private void drawSettings() {
        fov.draw();
        sensitivity.draw();
        speed.draw();

        skybox.draw();
        world.draw();
        vsync.draw();
        wireframe.draw();
        directionCulling.draw();
        backfaceCulling.draw();

        chunkRadius.draw();
        blockCount.draw();
    }

}
