package edu.kit.scc.git.ggd.voxelite.ui;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.input.InputListener;
import edu.kit.scc.git.ggd.voxelite.render.*;
import edu.kit.scc.git.ggd.voxelite.util.*;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.CompressedLightStorage;
import edu.kit.scc.git.ggd.voxelite.world.WorldChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.NaturalWorldGenerator;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.LinearSpline;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.implot.ImPlot;
import imgui.extension.implot.ImPlotContext;
import imgui.extension.implot.flag.ImPlotAxisFlags;
import imgui.extension.implot.flag.ImPlotFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.internal.ImGuiContext;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec4f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.window.Window;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class UserInterface {
    private static final boolean SAVE_GUI = true;
    public static final Executor EXECUTOR = Executors.newSingleThreadScheduledExecutor(Util.DAEMON_THREAD_FACTORY);

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3  imGuiGl3  = new ImGuiImplGl3();
    private ImGuiContext  imGuiContext;
    private ImPlotContext imPlotContext;

    private final Accordion camera, world, generator, render, post, shadow, cull, vl, light, perf;

    private final LongRingBuffer loadQueueRingBuffer = new SuppliedLongRingBuffer(() -> Main.INSTANCE.getWorld().getLoadQueueSize());
    private final LongRingBuffer buildRingBuffer     = new SuppliedLongRingBuffer(() -> Main.INSTANCE.getRenderer().getWorldRenderer().getBuildQueueSize());
    private final LongRingBuffer uploadRingBuffer    = new SuppliedLongRingBuffer(() -> Main.INSTANCE.getRenderer().getWorldRenderer().getUploadQueueSize());
    private final LongRingBuffer memoryRingBuffer = new SuppliedLongRingBuffer(() -> (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1_000_000);

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
            var normalMap = new CheckboxElement("Normal Map", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().normalMap = value);
            var fog = new CheckboxElement("Fog", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().fog = value);
            var ao = new CheckboxElement("AO", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().ao = value);
            var aa = new CheckboxElement("AA", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().aliasingOn = value);
            var transparentSort = new CheckboxElement("Transparent sort", false, value -> Main.INSTANCE.getRenderer().getWorldRenderer().transparentSort = value);
            var frustumDebug = new CheckboxElement("Debug Frustum", false, value -> Main.INSTANCE.getRenderer().getWorldRenderer().debugFrustum = value);
            var frustumCapture = new CheckboxElement("Capture Frustum", false, value -> Main.INSTANCE.getRenderer().getWorldRenderer().captureFrustum = value);
            var ticksPerDay = new IntSliderElement("Day Length", 2000, 200, 20000, value -> Main.ticksPerDay = value);
            var roughness = new FloatSliderElement("Roughness Delta", 0, -2f, 2f, value -> Main.INSTANCE.getRenderer().getWorldRenderer().debugRoughness = value);
            var reflections = new CheckboxElement("SSR", false, value -> Main.INSTANCE.getRenderer().getWorldRenderer().reflections = value);
            var coneTracing = new CheckboxElement("CT", false, value -> Main.INSTANCE.getRenderer().getWorldRenderer().coneTracing = value);

            this.render = new Accordion("Render", true, skybox, ImGui::sameLine, world, ImGui::sameLine, vsync, ImGui::sameLine, wireframe, ImGui::sameLine, transparentSort,
                    frustumDebug, ImGui::sameLine, frustumCapture,  ImGui::sameLine, ao, ImGui::sameLine, aa, ImGui::sameLine, fog,
                    normalMap, ImGui::sameLine, reflections, ImGui::sameLine, coneTracing,
                    ticksPerDay,
                    roughness
            );
        }

        {
            var bloom = new CheckboxElement("Bloom", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getPostRenderer().bloom = value);
            var bloomBlur = new IntSliderElement("Bloom Blurs", 1, 0, 20, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getPostRenderer().bloomBlurIterations  = value);
            var bloomIntensity = new FloatSliderElement("Bloom Intensity", 3, 0, 10, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getPostRenderer().bloomIntensity = value);

            var hdr = new CheckboxElement("HDR", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getPostRenderer().hdr = value);
            var exposure = new FloatSliderElement("HDR Exposure", 1, 0, 2, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getPostRenderer().exposure = value);

            var gammaCorrect = new CheckboxElement("Gamma Correction", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getPostRenderer().gammaCorrect = value);
            var gamma = new FloatSliderElement("Gamma", 1, 0, 3, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getPostRenderer().gamma = value);

            this.post = new Accordion("Post-Processing", true, bloom, bloomIntensity, bloomBlur, hdr, exposure, gammaCorrect, gamma);
        }

        {
            var enabled = new CheckboxElement("Enabled", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().shadows = value);
            var transform = new CheckboxElement("Transform", false, value -> Main.INSTANCE.getRenderer().getWorldRenderer().shadowTransform = value);
            var frustumCull = new CheckboxElement("Frustum Cull", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getShadowMapRenderer().frustumCull = value);
            var hardwareFilter = new CheckboxElement("Hardware PCF", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getShadowMapRenderer().hardwareFiltering(value));
            var cascadeDebug = new CheckboxElement("Cascade Debug", false, value -> {
                RenderType.OPAQUE.getProgram().use(() -> {
                    RenderType.OPAQUE.getProgram().cascadeDebug.set(value ? 1 : 0);
                });
            });
            var frustumCount = new IntSliderElement("Frustum Count", 4, 1, 4, value -> {
                Main.INSTANCE.getRenderer().getWorldRenderer().getShadowMapRenderer().cascades = value;
                Main.INSTANCE.getRenderer().getWorldRenderer().getShadowMapRenderer().allocate();
            });
            var frustumNumber = new IntSliderElement("Frustum Number", 0, 0, 3, value -> WorldRenderer.frustumNumber = value);
            var kernel = new IntSliderElement("PCF Kernel", 2, 0, 10, value -> {
                RenderType.OPAQUE.getProgram().use(() -> {
                    RenderType.OPAQUE.getProgram().kernel.set(value);
                });
            });
            var resolution = new IntSliderElement("Resolution Exponent", 12, 5, 16, value -> {
                final ShadowMapRenderer shadowMapRenderer = Main.INSTANCE.getRenderer().getWorldRenderer().getShadowMapRenderer();
                shadowMapRenderer.resolution = 1 << value;
                shadowMapRenderer.allocate();
            });
            var precision = new DropdownElement<GLTexture.Format>("Precision Exponent",
                    Map.of("Default", GLTexture.BaseFormat.DEPTH_COMPONENT,
                            "16", GLTexture.SizedFormat.DEPTH_16,
                            "24", GLTexture.SizedFormat.DEPTH_24,
                            "32", GLTexture.SizedFormat.DEPTH_32,
                            "32F", GLTexture.SizedFormat.DEPTH_32F),
                    value -> Main.INSTANCE.getRenderer().getWorldRenderer().getShadowMapRenderer().depthFormat(value));
            var cullStats = new TextElement(() -> {
                final StringBuilder sb = new StringBuilder();
                final float totalChunks = Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunks().size();
                final ShadowMapRenderer shadowMapRenderer = Main.INSTANCE.getRenderer().getWorldRenderer().getShadowMapRenderer();
                for (int c = 0; c < shadowMapRenderer.cascades; c++) {
                    if (c > 0) sb.append(" | ");
                    sb.append(c).append(": ").append(shadowMapRenderer.cullCounts[c]).append(" (").append("%.1f%%".formatted(100 * shadowMapRenderer.cullCounts[c] / totalChunks)).append(")");
                }
                return sb.toString();
            });
            var constantBias = new FloatSliderElement("Constant Bias", 0f, -1f, 1f, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getShadowMapRenderer().constantBias = value * 0.001f);
            var splitCorrection = new FloatSliderElement("Split correction", 0.9f, 0, 1f, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getShadowMapRenderer().splitCorrection = value);
            this.shadow = new Accordion("Shadow", true, enabled, ImGui::sameLine, transform, ImGui::sameLine, frustumCull, ImGui::sameLine, hardwareFilter, ImGui::sameLine, cascadeDebug,
                    frustumCount,
                    frustumNumber,
                    kernel,
                    resolution,
                    precision,
                    constantBias,
                    splitCorrection,
                    cullStats);
        }

        {
            var caveCull = new CheckboxElement("Cave", false, value -> Main.INSTANCE.getRenderer().getWorldRenderer().caveCull = value);
            var dotCull = new CheckboxElement("Dot", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().dotCull = value);
            var frustumCull = new CheckboxElement("Frustum", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().frustumCull = value);
            var occlusionCull = new CheckboxElement("Occlusion", false, value -> Main.INSTANCE.getRenderer().getWorldRenderer().occlusionCull = value);
            var directionCull = new CheckboxElement("Direction", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().directionCull = value);
            var backfaceCull = new CheckboxElement("Backface", true, value -> Main.INSTANCE.getRenderer().getWorldRenderer().backfaceCull = value);
            var occlusionCullThreshold = new IntSliderElement("Occlusion Threshold", 0, 0, (Chunk.VOLUME * 6) / 2, value -> Main.INSTANCE.getRenderer().getWorldRenderer().occlusionCullThreshold = value);
            var cullStats = new TextElement(() -> {
                final float chunks = Main.INSTANCE.getWorld().getChunks().size();
                final WorldRenderer renderer = Main.INSTANCE.getRenderer().getWorldRenderer();

                return "Empty: %d (%.1f%%) | Cave: %d (%.1f%%) %nDot: %d (%.1f%%) | Frustum: %d (%.1f%%) %nOcclusion: %d (%.1f%%) | Total: %d (%.1f%%)".formatted(
                        renderer.emptyCount, 100 * renderer.emptyCount / chunks,
                        renderer.caveCullCount, 100 * renderer.caveCullCount / chunks,
                        renderer.dotCullCount, 100 * renderer.dotCullCount / chunks,
                        renderer.frustumCullCount, 100 * renderer.frustumCullCount / chunks,
                        renderer.occlusionCullCount, 100 * renderer.occlusionCullCount / chunks,
                        renderer.totalCullCount, 100 * renderer.totalCullCount / chunks
                );
            });

            this.cull = new Accordion("Culling", false, caveCull, ImGui::sameLine, dotCull, ImGui::sameLine, frustumCull, ImGui::sameLine, occlusionCull, ImGui::sameLine, directionCull, ImGui::sameLine, backfaceCull,
                    occlusionCullThreshold,
                    cullStats);
        }

        {
            var samples = new IntSliderElement("Samples", 50, 0, 200, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getCompositeRenderer().godraySamples = value);
            var density = new FloatSliderElement("Density", 1, 0, 1, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getCompositeRenderer().godrayDensity = value);
            var decay = new FloatSliderElement("Decay", 1, 0, 10, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getCompositeRenderer().godrayDecay = value);
            var exposure = new FloatSliderElement("VL Exposure", 0.05f, 0, 1, value -> Main.INSTANCE.getRenderer().getWorldRenderer().getCompositeRenderer().godrayExposure = value);

            this.vl = new Accordion("Volumetric Lighting", true, samples, density, decay, exposure);
        }

        {
            var load = new CheckboxElement("Update Area", true, value -> Main.INSTANCE.getWorld().setLoadChunks(value));
            var radius = new IntSliderElement("Chunk radius", 4, 0, 50, value -> Main.INSTANCE.getWorld().setChunkRadius(value));
            var sortRate = new IntSliderElement("Sort rate", 8, 0, 64, value -> Main.INSTANCE.getRenderer().getWorldRenderer().sortRate = value);
            var buildRate = new IntSliderElement("Upload rate", 8, 0, 64, value -> Main.INSTANCE.getRenderer().getWorldRenderer().uploadRate = value);
            var uploadRate = new IntSliderElement("Build rate", 8, 0, 64, value -> Main.INSTANCE.getWorld().buildRate = value);
            var rebuild = new ButtonElement("Force rebuild", () -> Main.INSTANCE.getRenderer().getWorldRenderer().queueAll());
            var regenerate = new ButtonElement("Force regenerate", () -> Main.INSTANCE.getWorld().regenerate());
            this.world = new Accordion("World", true, load, radius, buildRate, uploadRate, sortRate, rebuild, ImGui::sameLine, regenerate);
        }

        {
            var output = new TextElement(() -> {
                final NaturalWorldGenerator g = Main.INSTANCE.getWorld().getGenerator();
                final Vec3f position = Main.INSTANCE.getRenderer().getCamera().getPosition();
                final NaturalWorldGenerator.NoisePoint noisePoint = g.sampleNoises(position);

                return "Biome: %s (C: %.2f | E: %.2f | R: %.2f | T: %.2f | H: %.2f)".formatted(g.selectBiome(noisePoint).name(), noisePoint.continentalness(), noisePoint.erosion(), noisePoint.ridge(), noisePoint.temperature(), noisePoint.humidity());
            });

            var noiseSelector = new DropdownElement<Supplier<Noise>>("Noise", Map.of(
                    "continentalness", () -> Main.INSTANCE.getWorld().getGenerator().getContinentalness(),
                    "erosion", () -> Main.INSTANCE.getWorld().getGenerator().getErosion(),
                    "ridge", () -> Main.INSTANCE.getWorld().getGenerator().getRidge(),
                    "temperature", () -> Main.INSTANCE.getWorld().getGenerator().getTemperature())
            );

            var noiseImageRenderer = new Function<Vec2i, BufferedImage>() {
                private float zoom = 1;
                private boolean spline = false;

                @Override
                public BufferedImage apply(Vec2i size) {
                    var g = Main.INSTANCE.getWorld().getGenerator();
                    var noise = noiseSelector.read().get();
                    var currentPosition = Main.INSTANCE.getRenderer().getCamera().getPosition();

                    if (spline) {
                        var s = g.getBaseHeightSpline();
                        //TODO Bad assumption
                        var min = s.sample(new NaturalWorldGenerator.NoisePoint(-1, 1, -1, 0, 0));
                        var max = s.sample(new NaturalWorldGenerator.NoisePoint(1, -1, 1, 0, 0));

                        return NoiseImageGenerator.generate(position -> {
                            var height = g.getBaseHeight(position);

                            return (height - min) / (max - min);
                        }, currentPosition, size, zoom);
                    } else {
                       return NoiseImageGenerator.generate(position -> {
                            var n = noise.sample(position);
                            return 0.5f * n + 0.5f;
                        }, currentPosition, size, zoom);
                    }
                }
            };

            var noiseImage = new ImageElement("Noise Image", () -> new Vec2i(ImGui.getColumnWidth(), 500), noiseImageRenderer);
            var zoom = new FloatSliderElement("Zoom", 1f, 0.001f, 2f, value -> noiseImageRenderer.zoom = value);
            var applySpline = new CheckboxElement("Apply Spline", false, value -> noiseImageRenderer.spline = value);

            var spline = new Element() {
                @Override
                public void draw() {
                    final Object baseHeightSpline = Main.INSTANCE.getWorld().getGenerator().getBaseHeightSpline();

                    if(baseHeightSpline instanceof LinearSpline l) {
                        if(ImPlot.beginPlot("Spline")) {
                            ImPlot.getStyle().setAntiAliasedLines(true);
                            final var x = IntStream.range(0, l.getX().length).mapToDouble(i -> l.getX()[i]).boxed().toArray(Double[]::new);
                            final var y = IntStream.range(0, l.getY().length).mapToDouble(i -> l.getY()[i]).boxed().toArray(Double[]::new);
                            ImPlot.plotLine("Line", x, y);
                            ImPlot.endPlot();
                        }
                    }
                }
            };

            var noiseSlice = new Element() {
                record PlotData(Double[] axis, Double[] x, Double[] z) {}

                final AsyncProducer<PlotData> asyncProducer = new AsyncProducer<>(this::sample, EXECUTOR);

                private PlotData sample() {
                    final int range = (int) (500f / zoom.read());
                    final int samples = range << 1;
                    final Double[] x = new Double[samples];
                    final Double[] z = new Double[samples];
                    final Noise noise = noiseSelector.read().get();

                    for (int i = 0; i < samples; i++) {
                        final Vec3f position = Main.INSTANCE.getRenderer().getCamera().getPosition();
                        final float offset = i - (samples >> 1);
                        x[i] = (double) noise.sample(position.xz().add(new Vec2f(1, 0).scale(offset)));
                        z[i] = (double) noise.sample(position.xz().add(new Vec2f(0, 1).scale(offset)));
                    }

                    final Double[] axis = IntStream.range(-(samples >> 1), samples >> 1).mapToDouble(value -> value).boxed().toArray(Double[]::new);
                    return new PlotData(axis, x, z);
                }

                @Override
                public void draw() {
                    final int axisFlags = ImPlotAxisFlags.AutoFit | ImPlotAxisFlags.NoLabel;
                    if(ImPlot.beginPlot("Noise Slice", "", "", new ImVec2(ImGui.getColumnWidth(), 200), ImPlotFlags.CanvasOnly, axisFlags, axisFlags)) {
                        var data = asyncProducer.get();
                        ImPlot.plotLine("Along X", data.axis, data.x);
                        ImPlot.plotLine("Along Z", data.axis, data.z);
                        ImPlot.endPlot();
                    }
                }
            };

            this.generator = new Accordion("Noise", true, output, noiseImage, zoom, applySpline, noiseSelector, spline, noiseSlice);
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
        imGuiContext = ImGui.createContext();
        imPlotContext = ImPlot.createContext();
        ImPlot.getStyle().setAntiAliasedLines(true);

        if (!SAVE_GUI) ImGui.getIO().setIniFilename(null);
        imGuiGlfw.init(Main.INSTANCE.getWindow().id(), true);
        imGuiGl3.init("#version 430");
    }

    public void shutdown() {
        ImPlot.destroyContext(imPlotContext);
        ImGui.destroyContext();
    }

    public void tick() {
        loadQueueRingBuffer.tick();
        buildRingBuffer.tick();
        uploadRingBuffer.tick();
        memoryRingBuffer.tick();
    }

    public void draw() {
        OpenGL.colorMask(true);
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        ImGui.begin("Settings");

        drawProfiler();
        camera.draw();
        render.draw();
        post.draw();
        shadow.draw();
        cull.draw();
        vl.draw();
        world.draw();
        light.draw();
        perf.draw();
        generator.draw();

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
                        Main.INSTANCE.getWorld().getChunks().stream().mapToInt(WorldChunk::getBlockCount).sum(),
                        Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunks().stream().mapToInt(RenderChunk::getQuadCount).sum()
                ));

        float[] data = profiler.toArray(true);
        for (int i = 0; i < data.length; i++) {
            data[i] /= 1_000_000f;
        }
        ImGui.plotHistogram("", data, data.length, 0, "", 0, 16, ImGui.getColumnWidth(), 100);
    }
}
