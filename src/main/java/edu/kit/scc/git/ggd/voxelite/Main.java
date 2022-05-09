package edu.kit.scc.git.ggd.voxelite;

import edu.kit.scc.git.ggd.voxelite.input.InputListener;
import edu.kit.scc.git.ggd.voxelite.render.Renderer;
import edu.kit.scc.git.ggd.voxelite.util.Profiler;
import edu.kit.scc.git.ggd.voxelite.world.World;
import edu.kit.scc.git.ggd.voxelite.world.generator.ModuloChunkGenerator;
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

import static org.lwjgl.glfw.GLFW.*;

public class Main {
    private static final Logger LOGGER;

    private final Window        window;
    private final InputSystem   inputSystem;
    private final InputListener inputListener;
    private final Renderer      renderer;
    private final Profiler      profiler = new Profiler();
    private final World         world    = new World(new ModuloChunkGenerator());

    static {
        System.setProperty("log4j.skipJansi", "false");
        LOGGER = LoggerFactory.getLogger(Main.class);
    }

    //TODO Eliminate reference leaks
    private Main() {
        LOGGER.info("Initialization...");

        //Init
        Util.windowsTimerHack();
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Failed to initialize GLFW");

        //Window
        glfwDefaultWindowHints();
        window = Window.builder().build();
        window.setCursorMode(Window.CursorMode.DISABLED);
        window.setRawMouseInput(true);

        //Context
        window.makeContextCurrent();
        GL.createCapabilities();

        //Renderer
        try {
            renderer = new Renderer(this);
            renderer.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Input
        inputSystem = new InputSystem(window);
        inputListener = new InputListener(this);
        EventType.addListener(inputListener);

        //World
        world.getGenerator().setWorld(world);
    }

    public void run() {
        LOGGER.info("Run...");

        while (!window.shouldClose()) {
            profiler.tick();
            inputSystem.poll();
            inputSystem.tick();

            renderer.render();

            window.swapBuffers();
            OpenGL.clearAll();
        }

        LOGGER.info("Shutdown...");
        renderer.shutdown();
        glfwTerminate();
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

    public Profiler getProfiler() {
        return profiler;
    }

    public World getWorld() {
        return world;
    }

    public static void main(String[] args) {
        new Main().run();
    }
}