package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;

public class CompositeRenderer {

    public static final CompositeProgram PROGRAM = new CompositeProgram();

    public static final CompositeProgram.QuadVertex[] QUAD_VERTICES = new CompositeProgram.QuadVertex[]{
            new CompositeProgram.QuadVertex(new Vec2f(-1, -1)),
            new CompositeProgram.QuadVertex(new Vec2f(1, -1)),
            new CompositeProgram.QuadVertex(new Vec2f(-1, 1)),
            new CompositeProgram.QuadVertex(new Vec2f(1, 1))
    };

    public static final VertexBuffer<CompositeProgram.QuadVertex> VB = new VertexBuffer<>(CompositeProgram.QuadVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STATIC_DRAW);

    static {
        VB.use(() -> VB.data(QUAD_VERTICES));
    }

    private final VertexArray va = new VertexArray();

    public CompositeRenderer() {
        OpenGL.use(va, VB, () -> va.set(PROGRAM.pos, CompositeProgram.QuadVertex.POSITION, VB, 0));
    }

    public void render(GeometryBuffer gBuffer) {
        OpenGL.depthTest(true); //Must be enabled for depth writing
        OpenGL.depthFunction(OpenGL.CompareFunction.ALWAYS); //Disable depth testing the other way
        OpenGL.depthMask(true); //Write gBuffer depth to default framebuffer
        OpenGL.colorMask(true);
        OpenGL.blend(false);

        OpenGL.use(PROGRAM, va, () -> {
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

            OpenGL.drawArrays(OpenGL.Mode.TRIANGLE_STRIP, 0, 4);
        });

        OpenGL.depthFunction(OpenGL.CompareFunction.LESS_EQUAL); //TODO Eliminate reset responsibility
    }

}
