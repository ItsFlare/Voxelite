package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;
import net.durchholz.beacon.render.opengl.textures.ArrayTexture2D;

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

    public void render(GeometryBuffer gBuffer, ArrayTexture2D shadowMap) {
        OpenGL.depthTest(false);
        OpenGL.depthMask(false);
        OpenGL.blend(false);

        OpenGL.use(PROGRAM, va, () -> {
            final Camera camera = Main.INSTANCE.getRenderer().getCamera();
            PROGRAM.debugRoughness.set(Main.INSTANCE.getRenderer().getWorldRenderer().debugRoughness);
            PROGRAM.opaque.bind(0, gBuffer.opaque());
            PROGRAM.transparent.bind(1, gBuffer.transparent());
            PROGRAM.normal.bind(2, gBuffer.normal());
            PROGRAM.mer.bind(3, gBuffer.mer());
            PROGRAM.depth.bind(5, gBuffer.depth());
            PROGRAM.projection.set(camera.projection());
//            PROGRAM.shadowMap.bind(5, shadowMap);

            OpenGL.drawArrays(OpenGL.Mode.TRIANGLE_STRIP, 0, 4);
        });
    }

}
