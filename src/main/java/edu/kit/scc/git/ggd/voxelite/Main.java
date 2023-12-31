package edu.kit.scc.git.ggd.voxelite;

import edu.kit.scc.git.ggd.voxelite.input.InputListener;
import edu.kit.scc.git.ggd.voxelite.render.Renderer;
import edu.kit.scc.git.ggd.voxelite.util.TimerRingBuffer;
import edu.kit.scc.git.ggd.voxelite.util.VoxeliteExecutor;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.World;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.NaturalWorldGenerator;
import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.input.InputSystem;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.util.Util;
import net.durchholz.beacon.window.Window;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL30.glDisable;

public class Main {
    public static final Main INSTANCE;

    public static final int  TICKRATE      = 20;
    public static final long NS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);
    public static final long NS_PER_TICK   = NS_PER_SECOND / TICKRATE;

    private static final Logger LOGGER;

    private final Window           window;
    private final InputSystem      inputSystem;
    private final InputListener    inputListener;
    private final Renderer         renderer;
    private final TimerRingBuffer  profiler = new TimerRingBuffer();
    private final VoxeliteExecutor executor = new VoxeliteExecutor();
    private World world;
    private long  frameTime = TimeUnit.MILLISECONDS.toNanos(16);

    private       long tick;
    public static long ticksPerDay = 2000;

    static {
        System.setProperty("log4j.skipJansi", "false");
        LOGGER = LoggerFactory.getLogger(Main.class);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            LOGGER.error("Uncaught exception in Thread %s".formatted(t), e);
        });
        INSTANCE = new Main();
    }

    private Main() {
        LOGGER.info("Construction...");

        Util.windowsTimerHack();
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Failed to initialize GLFW");

        //Window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_RED_BITS, 16);
        glfwWindowHint(GLFW_GREEN_BITS, 16);
        glfwWindowHint(GLFW_BLUE_BITS, 16);
        window = Window.builder().title("Voxelite").build();
        window.setCursorMode(Window.CursorMode.DISABLED);
        window.setRawMouseInput(true);

        //Context
        window.makeContextCurrent();
        GL.createCapabilities();
        glDisable(GL_MULTISAMPLE);
        OpenGL.setExecutor(executor);

        //Renderer
        try {
            renderer = new Renderer(window);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Input
        inputSystem = new InputSystem(window);
        inputListener = new InputListener();
    }

    private void init() {
        LOGGER.info("Initialization...");
        renderer.init();
        EventType.addListener(inputListener);

        /*
        Initialize some classes on main thread to fail-fast.
        Prevents errors from being suppressed in other threads.
        */
        Util.initialize(Block.class);

        //World
        world = new World(new NaturalWorldGenerator(0));
        world.getGenerator().setWorld(world);
    }

    public void run() {
        init();
        window.show();

        LOGGER.info("Run...");
        long accumulator = 0, deltaTime = 0;
        while (!window.shouldClose()) {
            long start = System.nanoTime();
            profiler.tick();

            inputSystem.poll();
            inputSystem.tick();
            inputListener.move(deltaTime / (float) NS_PER_SECOND);

            while (accumulator >= NS_PER_TICK) {
                simulate();
                accumulator -= NS_PER_TICK;
            }

            world.frame();
            renderer.render();

            window.swapBuffers();
            OpenGL.colorMask(true);
            OpenGL.depthMask(true);
            OpenGL.clearAll();

            executor.process();
            deltaTime = System.nanoTime() - start;
            accumulator += deltaTime;
            frameTime = deltaTime;
        }

        LOGGER.info("Shutdown...");
        renderer.shutdown();
        glfwTerminate();
    }

    private void simulate() {
        world.tick();
        renderer.tick();
        tick++;
    }

    public Window getWindow() {
        return window;
    }

    public InputSystem getInputSystem() {
        return inputSystem;
    }

    public InputListener getInputListener() {
        return inputListener;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public TimerRingBuffer getProfiler() {
        return profiler;
    }

    public World getWorld() {
        return world;
    }

    public VoxeliteExecutor getExecutor() {
        return executor;
    }

    public long getFrameTime() {
        return frameTime;
    }

    public long getTick() {
        return tick;
    }

    public static float getDayPercentage() {
        return (Main.INSTANCE.getTick() % Main.ticksPerDay) / (float) Main.ticksPerDay;
    }

    public static void main(String[] args) {
        INSTANCE.run();
    }
}