package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.*;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;

public class CompositeRenderer extends ScreenRenderer {

    public static final CompositeProgram PROGRAM = new CompositeProgram();


    public CompositeRenderer() {
        super(PROGRAM);
    }

    public void render(GeometryBuffer gBuffer, FBO outputFrameBuffer) {
        OpenGL.use(OpenGL.STATE, PROGRAM, va, outputFrameBuffer, () -> {
            OpenGL.resetState();
            OpenGL.depthTest(true);
            OpenGL.depthFunction(OpenGL.CompareFunction.ALWAYS); //Disable depth testing the other way

            final Camera camera = Main.INSTANCE.getRenderer().getCamera();
            final WorldRenderer worldRenderer = Main.INSTANCE.getRenderer().getWorldRenderer();
            PROGRAM.debugRoughness.set(worldRenderer.debugRoughness);
            PROGRAM.opaque.bind(0, gBuffer.opaque());
            PROGRAM.normal.bind(1, gBuffer.normal());
            PROGRAM.mer.bind(2, gBuffer.mer());
            PROGRAM.depth.bind(3, gBuffer.depth());
            PROGRAM.projection.set(camera.projection());
            PROGRAM.reflections.set(worldRenderer.reflections ? 1 : 0);
            PROGRAM.coneTracing.set(worldRenderer.coneTracing ? 1 : 0);

            drawScreen();
        });
    }
}
