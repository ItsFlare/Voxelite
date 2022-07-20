package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;

public class ScreenRenderer {

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

    protected final VertexArray va = new VertexArray();

    protected final ScreenProgram program;


    public ScreenRenderer(ScreenProgram program) {
        this.program = program;
        OpenGL.use(va, VB, () -> va.set(program.pos, CompositeProgram.QuadVertex.POSITION, VB, 0));
    }

    protected void drawScreen() {
        OpenGL.drawArrays(OpenGL.Mode.TRIANGLE_STRIP, 0, QUAD_VERTICES.length);
    }
}
