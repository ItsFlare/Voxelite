package edu.kit.scc.git.ggd.voxel;

import edu.kit.scc.git.ggd.voxel.input.InputListener;
import edu.kit.scc.git.ggd.voxel.ui.UserInterface;
import edu.kit.scc.git.ggd.voxel.util.Profiler;
import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.input.InputSystem;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.util.Util;
import net.durchholz.beacon.window.Window;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;

public class Main {
    public static final Window        WINDOW;
    public static final InputSystem   INPUT_SYSTEM;
    public static final InputListener INPUT_LISTENER;
    public static final UserInterface USER_INTERFACE;
    public static final Profiler      PROFILER = new Profiler();

    static {
        //Init
        Util.windowsTimerHack();
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Failed to initialize GLFW");

        //Window
        glfwDefaultWindowHints();
        WINDOW = Window.builder().build();

        //Context
        WINDOW.makeContextCurrent();
        GL.createCapabilities();

        //Input
        INPUT_SYSTEM = new InputSystem(WINDOW);
        INPUT_LISTENER = new InputListener();
        EventType.addListener(INPUT_LISTENER);

        //UI
        USER_INTERFACE = new UserInterface(WINDOW);
        USER_INTERFACE.init();
    }

    public static void main(String[] args) {
        while (!WINDOW.shouldClose()) {
            PROFILER.tick();
            INPUT_SYSTEM.poll();
            INPUT_SYSTEM.tick();
            USER_INTERFACE.tick();

            WINDOW.swapBuffers();
            OpenGL.clearAll();
        }

        USER_INTERFACE.shutdown();
        glfwTerminate();
    }
}