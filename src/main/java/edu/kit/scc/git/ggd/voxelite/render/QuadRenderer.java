package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.IBO;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;
import net.durchholz.beacon.render.opengl.textures.Texture2D;

import static net.durchholz.beacon.render.opengl.OpenGL.*;

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
    private final VertexBuffer<QuadProgram.QuadVertex> vb = new VertexBuffer<>(QuadProgram.QuadVertex.LAYOUT, BufferLayout.INTERLEAVED, Usage.STATIC_DRAW);
    private final IBO ibo = new IBO();

    public QuadRenderer() {
        use(va, ibo, () -> {
            vb.use(() -> {
                vb.data(VERTICES);
                va.set(program.pos, QuadProgram.QuadVertex.POSITION, vb, 0);
                va.set(program.uv, QuadProgram.QuadVertex.UV, vb, 0);
            });

            ibo.data(Usage.STATIC_DRAW, INDICES);
        });
    }

    public void render(Matrix4f matrix, Texture2D texture, Vec2f offset, Vec2f size) {
        use(STATE, program, va, () -> {
            depthTest(false);
            depthMask(false);
            cull(false);

            program.mvp.set(matrix);

            program.sampler.bind(0, texture);
            program.size.set(size);
            program.offset.set(offset);

            drawIndexed(Mode.TRIANGLES, INDICES.length, Type.UNSIGNED_INT);
        });
    }
}