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
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec4f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.window.Window;

public class UserInterface {
    private static final boolean SAVE_GUI = true;

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3  imGuiGl3  = new ImGuiImplGl3();

    private final Accordion camera, world, render, light;
    
    public UserInterface() {
        {
            var fov = new IntSliderElement("FOV", Camera.DEFAULT_FOV, 5, 180, value -> Main.INSTANCE.getRenderer().getCamera().setFOV(value));
            var sensitivity = new FloatSliderElement("Sensitivity", InputListener.DEFAULT_SENSITIVITY, 0.01f, 10, value -> Main.INSTANCE.getInputListener().sensitivity = value);
            var speed = new FloatSliderElement("Speed", InputListener.DEFAULT_CAMERA_SPEED, 0.01f, 10, value -> Main.INSTANCE.getInputListener().cameraSpeed = value);
            this.camera = new Accordion("Camera", true, fov, sensitivity, speed);
        }

        {
            var skybox = new CheckboxElement("Skybox", true, value -> Main.INSTANCE.getRenderer().renderSkybox = value);
            var world = new CheckboxElement("World", true, value -> Main.INSTANCE.getRenderer().renderWorld = value);
            var vsync = new CheckboxElement("VSync", true, value -> Window.swapInterval(value ? 1 : 0));
            var wireframe = new CheckboxElement("Wireframe", false, value -> Main.INSTANCE.getRenderer().wireframe = value);
            var directionCulling = new CheckboxElement("Direction Culling", true, value -> ChunkProgram.directionCulling = value);
            var backfaceCulling = new CheckboxElement("Backface Culling", true, OpenGL::cull);
            this.render = new Accordion("Render", true, skybox, world, vsync, wireframe, directionCulling, backfaceCulling);
        }

        {
            var load = new CheckboxElement("Load Chunks", true, value -> Main.INSTANCE.getWorld().setLoadChunks(value));
            var radius = new IntSliderElement("Chunk radius", 2, 0, 50, value -> Main.INSTANCE.getWorld().setChunkRadius(value));
            var modulo = new IntSliderElement("Block gen modulo", ModuloChunkGenerator.DEFAULT_MOD, 1, 50, value -> {
                if (Main.INSTANCE.getWorld().getGenerator() instanceof ModuloChunkGenerator r) {
                    r.modulo = value;
                    Main.INSTANCE.getWorld().regenerate();
                }
            });
            var rebuild = new ButtonElement("Rebuild meshes", () -> Main.INSTANCE.getRenderer().getWorldRenderer().rebuildMeshes());
            this.world = new Accordion("World", true, load, radius, modulo, rebuild);
        }

        {
            var color = new ColorPickerElement("Light", new Vec4f(1), value -> Main.INSTANCE.getRenderer().getWorldRenderer().lightColor = new Vec3f(value.x(), value.y(), value.z()));
            var ambient = new FloatSliderElement("Ambient", 0.2f, 0, 1, value -> Main.INSTANCE.getRenderer().getWorldRenderer().ambientStrength = value);
            var diffuse = new FloatSliderElement("Diffuse", 0.5f, 0, 1, value -> Main.INSTANCE.getRenderer().getWorldRenderer().diffuseStrength = value);
            var specular = new FloatSliderElement("Specular", 0.5f, 0, 1, value -> Main.INSTANCE.getRenderer().getWorldRenderer().specularStrength = value);
            this.light = new Accordion("Light", false, color, ambient, diffuse, specular);
        }
    }

    public void init() {
        ImGui.createContext();
        if (!SAVE_GUI) ImGui.getIO().setIniFilename(null);
        imGuiGlfw.init(Main.INSTANCE.getWindow().id(), true);
        imGuiGl3.init("#version 430");
    }

    public void shutdown() {
        ImGui.destroyContext();
    }

    public void tick() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        ImGui.begin("Settings");

        drawProfiler();
        drawPosition();
        drawSettings();

        ImGui.end();
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void drawProfiler() {
        final float frameTime = Main.INSTANCE.getProfiler().frameTime();
        ImGui.text("%d FPS (AVG %.2fms | MIN %.2fms | MAX  %.2fms)".formatted((int) (1_000 / frameTime), frameTime, Main.INSTANCE.getProfiler().minFrameTime(), Main.INSTANCE.getProfiler().maxFrameTime()));
        ImGui.text("Chunks: %d | Blocks: %d | Quads: %d"
                .formatted(
                        Main.INSTANCE.getWorld().getChunks().size(),
                        Main.INSTANCE.getWorld().getChunks().stream().mapToInt(Chunk::getBlockCount).sum(),
                        Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunks().stream().mapToInt(RenderChunk::getQuadCount).sum()
                ));
    }

    private void drawPosition() {
        ImGui.text("Position: " + Main.INSTANCE.getRenderer().getCamera().getPosition());
        ImGui.text("Chunk: " + Chunk.toChunkPosition(Main.INSTANCE.getRenderer().getCamera().getPosition()));
    }

    private void drawSettings() {
        camera.draw();
        render.draw();
        world.draw();
        light.draw();
    }

}
