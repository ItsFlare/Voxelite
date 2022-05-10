package edu.kit.scc.git.ggd.voxelite;

import edu.kit.scc.git.ggd.voxelite.input.InputListener;
import edu.kit.scc.git.ggd.voxelite.render.Renderer;
import edu.kit.scc.git.ggd.voxelite.texture.TextureAtlas;
import edu.kit.scc.git.ggd.voxelite.util.Profiler;
import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.input.InputSystem;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.VBO;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.util.Util;
import net.durchholz.beacon.window.Window;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

public class Main {
    private static final Logger LOGGER;

    private final Window        window;
    private final InputSystem   inputSystem;
    private final InputListener inputListener;
    private final Renderer      renderer;
    private final Profiler      profiler = new Profiler();

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Input
        inputSystem = new InputSystem(window);
        inputListener = new InputListener(this);
        EventType.addListener(inputListener);
    }

    public void run() {
        renderer.init();

        LOGGER.info("Run...");

        try{
            TextureAtlas texture = new TextureAtlas("textures/wool");
            Shader vertex = Shader.vertex("#version 410 core\n" +
                    "layout (location = 0) in vec3 aPos;\n" +
                    "layout (location = 1) in vec2 aTexCoord;\n" +
                    "\n" +
                    "out vec2 TexCoord;\n" +
                    "\n" +
                    "uniform mat4 model;\n" +
                    "uniform mat4 view;\n" +
                    "uniform mat4 projection;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    gl_Position = vec4(aPos, 1.0);\n" +
                    "    TexCoord = vec2(aTexCoord.x, aTexCoord.y);\n" +
                    "}");
            Shader fragment = Shader.fragment(edu.kit.scc.git.ggd.voxelite.util.Util.readStringResource("shaders/FragmentShader"));
            Program program = new Program(vertex, fragment);
            Vec2f[] vec = texture.getNormCoord("green_wool.png");
            float[] vertices = {
                    -0.5f, -0.5f, -0.5f,  vec[0].x(), vec[0].y(),
                    0.5f, -0.5f, -0.5f,  vec[1].x(), vec[1].y(),
                    0.5f,  0.5f, -0.5f,  vec[3].x(), vec[3].y(),
                    0.5f,  0.5f, -0.5f,  vec[3].x(), vec[3].y(),
                    -0.5f,  0.5f, -0.5f, vec[2].x(), vec[2].y(),
                    -0.5f, -0.5f, -0.5f,  vec[0].x(), vec[0].y(),
            };

            VBO vbo = new VBO();
            glBindBuffer(GL_ARRAY_BUFFER, vbo.id());
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 20, 0);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 20, 12);
            glEnableVertexAttribArray(1);
            glBindTexture(GL_TEXTURE_2D, texture.id());
            glUseProgram(program.id());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        while (!window.shouldClose()) {
            profiler.tick();
            inputSystem.poll();
            inputSystem.tick();


            renderer.render();

            glDrawArrays(GL_TRIANGLES, 0 ,6);

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

    public static void main(String[] args) {
        new Main().run();
    }
}