package edu.kit.scc.git.ggd.voxelite.ui;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.input.InputListener;
import edu.kit.scc.git.ggd.voxelite.render.Camera;
import edu.kit.scc.git.ggd.voxelite.render.ChunkProgram;
import edu.kit.scc.git.ggd.voxelite.render.RenderChunk;
import edu.kit.scc.git.ggd.voxelite.util.LongRingBuffer;
import edu.kit.scc.git.ggd.voxelite.util.SuppliedLongRingBuffer;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.CompressedLightStorage;
import edu.kit.scc.git.ggd.voxelite.world.generator.NaturalWorldGenerator;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec4f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.window.Window;

import java.util.Map;
import java.util.function.Consumer;

public class UserInterface {
    private static final boolean SAVE_GUI = true;

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3  imGuiGl3  = new ImGuiImplGl3();

    private final Accordion camera, world, render, cull, light, perf;

    private final LongRingBuffer loadQueueRingBuffer = new SuppliedLongRingBuffer(() -> Main.INSTANCE.getWorld().getLoadQueueSize());
    private final LongRingBuffer buildRingBuffer     = new SuppliedLongRingBuffer(() -> Main.INSTANCE.getRenderer().getWorldRenderer().getBuildQueueSize());
    private final LongRingBuffer uploadRingBuffer    = new SuppliedLongRingBuffer(() -> Main.INSTANCE.getRenderer().getWorldRenderer().getUploadQueueSize());
    private final LongRingBuffer memoryRingBuffer    = new SuppliedLongRingBuffer(() -> (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1_000_000);

    public UserInterface() {
        {
            var blockPos = new TextElement(() -> "Position: " + Chunk.toBlockPosition(Main.INSTANCE.getRenderer().getCamera().getPosition()));
            var chunkPos = new TextElement(() -> "Chunk: " + Chunk.toChunkPosition(Main.INSTANCE.getRenderer().getCamera().getPosition()));
            var block = new TextElement(() -> {
                var voxel = Main.INSTANCE.getWorld().getVoxel(Main.INSTANCE.getRenderer().getCamera().getPosition());
                return "Block: " + (voxel == null ? "null" : voxel.getBlock());
            });
            var blockLight = new TextElement(() -> {
                var voxel = Main.INSTANCE.getWorld().getVoxel(Main.INSTANCE.getRenderer().getCamera().getPosition());
                return "Light: " + (voxel == null ? "null" : voxel.chunk().getLightStorage().getLight(voxel.position()));
            });

            var fov = new IntSliderElement("FOV", Camera.DEFAULT_FOV, 5, 180, value -> Main.INSTANCE.getRenderer().getCamera().setFOV(value));
            var sensitivity = new FloatSliderElement("Sensitivity", InputListener.DEFAULT_SENSITIVITY, 0f, 5f, value -> Main.INSTANCE.getInputListener().sensitivity = value);
            var speed = new IntSliderElement("Speed", InputListener.DEFAULT_CAMERA_SPEED, 1, 300, value -> Main.INSTANCE.getInputListener().cameraSpeed = value);
            this.camera = new Accordion("Camera", true, blockPos, chunkPos, block, blockLight, fov, sensitivity, speed);
        }

        {
            var skybox = new CheckboxElement("Skybox", true, value -> Main.INSTANCE.getRenderer().renderSkybox = value);
            var world = new CheckboxElement("World", true, value -> Main.INSTANCE.getRenderer().renderWorld = value);
            var vsync = new CheckboxElement("VSync", true, value -> Window.swapInterval(value ? 1 : 0));
            var wireframe = new CheckboxElement("Wireframe", false, value -> Main.INSTANCE.getRenderer().wireframe = value);


            this.render = new Accordion("Render", true, skybox, ImGui::sameLine, world, ImGui::sameLine, vsync, ImGui::sameLine, wireframe);
        }

        {
            var directionCull = new CheckboxElement("Direction", true, value -> ChunkProgram.directionCulling = value);
            var backfaceCull = new CheckboxElement("Backface", true, OpenGL::cull);
            var caveCull = new CheckboxElement("Cave", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().caveCull = value);
            var frustumCull = new CheckboxElement("Frustum", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().frustumCull = value);
            var cullStats = new TextElement(() -> "Cave: %d (%.1f%%) | Empty: %d (%.1f%%) %nFrustum: %d (%.1f%%) | Total: %d (%.1f%%)".formatted(
                    Main.INSTANCE.getRenderer().getWorldRenderer().caveCullCount, 100 * Main.INSTANCE.getRenderer().getWorldRenderer().caveCullCount / (float) Main.INSTANCE.getWorld().getChunks().size(),
                    Main.INSTANCE.getRenderer().getWorldRenderer().emptyCount, 100 * Main.INSTANCE.getRenderer().getWorldRenderer().emptyCount / (float) Main.INSTANCE.getWorld().getChunks().size(),
                    Main.INSTANCE.getRenderer().getWorldRenderer().frustumCullCount, 100 * Main.INSTANCE.getRenderer().getWorldRenderer().frustumCullCount / (float) Main.INSTANCE.getWorld().getChunks().size(),
                    Main.INSTANCE.getRenderer().getWorldRenderer().totalCullCount, 100 * Main.INSTANCE.getRenderer().getWorldRenderer().totalCullCount / (float) Main.INSTANCE.getWorld().getChunks().size()
            ));

            this.cull = new Accordion("Culling", false, directionCull, ImGui::sameLine, backfaceCull, ImGui::sameLine, caveCull, ImGui::sameLine, frustumCull, cullStats);
        }

        {
            var load = new CheckboxElement("Update Area", true, value -> Main.INSTANCE.getWorld().setLoadChunks(value));
            var radius = new IntSliderElement("Chunk radius", 4, 0, 50, value -> Main.INSTANCE.getWorld().setChunkRadius(value));
            var frequency = new FloatSliderElement("Frequency", 0.02f, 0, 0.1f, value -> {
                if(Main.INSTANCE.getWorld().getGenerator() instanceof NaturalWorldGenerator g) {
                    g.getPasses().get(0).setFrequency(value);
                }
            });
            var amplitude = new IntSliderElement("Amplitude", 20, 0, 50, value -> {
                if(Main.INSTANCE.getWorld().getGenerator() instanceof NaturalWorldGenerator g) {
                    g.getPasses().get(0).setAmplitude(value);
                }
            });
            var buildRate = new IntSliderElement("Upload rate", 8, 0, 64, value -> Main.INSTANCE.getRenderer().getWorldRenderer().uploadRate = value);
            var uploadRate = new IntSliderElement("Build rate", 8, 0, 64, value -> Main.INSTANCE.getWorld().buildRate = value);
            var rebuild = new ButtonElement("Force rebuild", () -> Main.INSTANCE.getRenderer().getWorldRenderer().queueAll());
            var regenerate = new ButtonElement("Force regenerate", () -> Main.INSTANCE.getWorld().regenerate());
            this.world = new Accordion("World", true, load, radius, frequency, amplitude, buildRate, uploadRate, rebuild, ImGui::sameLine, regenerate);
        }

        {
            var mode = new DropdownElement<Consumer<Vec4f>>("Mode",
                    Map.of("sun", value -> Main.INSTANCE.getRenderer().getWorldRenderer().lightColor = value,
                            "block", value -> {
                                Block.GLOWSTONE.light = new Vec3f(value.x(), value.y(), value.z());
                                Block.GLOWSTONE.compressedLight = CompressedLightStorage.encode(Block.GLOWSTONE.light, Block.GLOWSTONE.getLightRange());
                            }, "filter", value -> {
                                Block.RED_GLASS.filter = new Vec3f(value.x(), value.y(), value.z());
                                Block.RED_GLASS.compressedFilter = CompressedLightStorage.encode(Block.RED_GLASS.filter, CompressedLightStorage.MAX_COMPONENT_VALUE);
                            }
                    )
            );

            var color = new ColorPickerElement("Light", new Vec4f(1), value -> mode.read().accept(value));
            var ambient = new FloatSliderElement("Ambient", 0.4f, 0, 1, value -> Main.INSTANCE.getRenderer().getWorldRenderer().ambientStrength = value);
            var diffuse = new FloatSliderElement("Diffuse", 0.7f, 0, 1, value -> Main.INSTANCE.getRenderer().getWorldRenderer().diffuseStrength = value);
            var specular = new FloatSliderElement("Specular", 0.2f, 0, 1, value -> Main.INSTANCE.getRenderer().getWorldRenderer().specularStrength = value);
            var exponent = new IntSliderElement("Exponent", 32, 1, 128, value -> Main.INSTANCE.getRenderer().getWorldRenderer().phongExponent = value);
            this.light = new Accordion("Light", false, mode, color, ambient, diffuse, specular, exponent);
        }

        {
            final int height = 100;
            final String overlay = "CUR %d | MIN %d | MAX %d";

            var generate = new PlotElement(
                    "Generate",
                    PlotElement.Type.LINES,
                    height,
                    () -> loadQueueRingBuffer.toArray(true),
                    () -> overlay.formatted(loadQueueRingBuffer.get(), loadQueueRingBuffer.min(), loadQueueRingBuffer.max())
            );

            var build = new PlotElement(
                    "Build",
                    PlotElement.Type.LINES,
                    height,
                    () -> buildRingBuffer.toArray(true),
                    () -> overlay.formatted(buildRingBuffer.get(), buildRingBuffer.min(), buildRingBuffer.max())
            );

            var upload = new PlotElement(
                    "Upload",
                    PlotElement.Type.LINES,
                    height,
                    () -> uploadRingBuffer.toArray(true),
                    () -> overlay.formatted(uploadRingBuffer.get(), uploadRingBuffer.min(), uploadRingBuffer.max())
            );

            var memory = new PlotElement(
                    "Memory (MB)",
                    PlotElement.Type.LINES,
                    height,
                    () -> memoryRingBuffer.toArray(true),
                    () -> overlay.formatted(memoryRingBuffer.get(), memoryRingBuffer.min(), memoryRingBuffer.max())
            );

            var gc = new ButtonElement("Run GC", System::gc);

            this.perf = new Accordion("Performance Graphs", false, generate, build, upload, memory, gc);
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
        loadQueueRingBuffer.tick();
        buildRingBuffer.tick();
        uploadRingBuffer.tick();
        memoryRingBuffer.tick();
    }

    public void draw() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        ImGui.begin("Settings");

        drawProfiler();
        camera.draw();
        render.draw();
        cull.draw();
        world.draw();
        light.draw();
        perf.draw();

        ImGui.end();
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void drawProfiler() {
        LongRingBuffer profiler = Main.INSTANCE.getProfiler();
        final float frameTime = profiler.average() / 1_000_000f;
        ImGui.text("%d FPS (AVG %.2fms | MIN %.2fms | MAX  %.2fms | JIT %.2fms)".formatted((int) (1_000 / frameTime), frameTime, profiler.min() / 1_000_000f, profiler.max() / 1_000_000f, profiler.jitter() / 1_000_000f));
        ImGui.text("Chunks: %d (%d) | Blocks: %d | Quads: %d"
                .formatted(
                        Main.INSTANCE.getWorld().getChunks().size(),
                        Main.INSTANCE.getRenderer().getWorldRenderer().renderList.size(),
                        Main.INSTANCE.getWorld().getChunks().stream().mapToInt(Chunk::getBlockCount).sum(),
                        Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunks().stream().mapToInt(RenderChunk::getQuadCount).sum()
                ));

        float[] data = profiler.toArray(true);
        for (int i = 0; i < data.length; i++) {
            data[i] /= 1_000_000f;
        }
        ImGui.plotHistogram("", data, data.length, 0, "", 0, 16, ImGui.getColumnWidth(), 100);
    }
}
