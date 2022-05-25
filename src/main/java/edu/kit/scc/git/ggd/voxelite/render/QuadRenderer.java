package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.IBO;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;

import java.io.IOException;

public class QuadRenderer {

    private final QuadProgram program = new QuadProgram();

    private static final QuadProgram.QuadVertex[] VERTICES = {
            new QuadProgram.QuadVertex(new Vec3f(0.5f, 0.5f, 0.0f)),
            new QuadProgram.QuadVertex(new Vec3f(0.5f, -0.5f, 0.0f)),
            new QuadProgram.QuadVertex(new Vec3f(-0.5f, -0.5f, 0.0f)),
            new QuadProgram.QuadVertex(new Vec3f(-0.5f, 0.5f, 0.0f)),
    };

    private static final int[] INDICES = {  // note that we start from 0!
            0, 1, 3,   // first triangle
            1, 2, 3    // second triangle
    };

    private final VertexArray                          va  = new VertexArray();
    private final VertexBuffer<QuadProgram.QuadVertex> vb  = new VertexBuffer<>(QuadProgram.QuadVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STATIC_DRAW);
    private final IBO                                  ibo = new IBO();

    public QuadRenderer() throws IOException {
        OpenGL.use(va, vb, ibo, () -> {
            vb.data(VERTICES);
            ibo.data(OpenGL.Usage.STATIC_DRAW, INDICES);
            va.set(program.pos, QuadProgram.QuadVertex.POSITION, vb, 0);
        });
    }

    public void render(Matrix4f matrix) {
        OpenGL.depthTest(false);
        OpenGL.depthMask(false);
        OpenGL.cull(false);

        OpenGL.use(program, va, () -> {
            program.mvp.set(matrix);

            OpenGL.drawIndexed(OpenGL.Mode.TRIANGLES, INDICES.length, OpenGL.Type.UNSIGNED_INT);
        });
    }
}