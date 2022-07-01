package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.IBO;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;
import net.durchholz.beacon.render.opengl.textures.Texture2D;

public class QuadRenderer {

    private final QuadProgram program = new QuadProgram();

    private static final QuadProgram.QuadVertex[] VERTICES = {
            new QuadProgram.QuadVertex(new Vec3f(1, 1, 0), new Vec2f(1, 1)),
            new QuadProgram.QuadVertex(new Vec3f(1, -1, 0), new Vec2f(1, 0)),
            new QuadProgram.QuadVertex(new Vec3f(-1, -1, 0), new Vec2f(0, 0)),
            new QuadProgram.QuadVertex(new Vec3f(-1, 1, 0), new Vec2f(0, 1)),
    };

    private static final int[] INDICES = {
            0, 1, 3,
            1, 2, 3
    };

    private final VertexArray va = new VertexArray();
    private final VertexBuffer<QuadProgram.QuadVertex> vb = new VertexBuffer<>(QuadProgram.QuadVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STATIC_DRAW);
    private final IBO ibo = new IBO();

    public QuadRenderer() {
        OpenGL.use(va, ibo, () -> {
            vb.use(() -> {
                vb.data(VERTICES);
                va.set(program.pos, QuadProgram.QuadVertex.POSITION, vb, 0);
                va.set(program.uv, QuadProgram.QuadVertex.UV, vb, 0);
            });

            ibo.data(OpenGL.Usage.STATIC_DRAW, INDICES);
        });
    }

    public void render(Matrix4f matrix, Texture2D texture, Vec2f offset, Vec2f size) {
        OpenGL.depthTest(false);
        OpenGL.depthMask(false);
        OpenGL.cull(false);

        OpenGL.use(program, va, () -> {
            program.mvp.set(matrix);

            program.sampler.bind(0, texture);
            program.size.set(size);
            program.offset.set(offset);

            OpenGL.drawIndexed(OpenGL.Mode.TRIANGLES, INDICES.length, OpenGL.Type.UNSIGNED_INT);
        });
    }
}