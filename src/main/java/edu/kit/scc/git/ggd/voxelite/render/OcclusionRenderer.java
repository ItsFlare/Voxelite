package edu.kit.scc.git.ggd.voxelite.render;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.durchholz.beacon.data.StructArray;
import net.durchholz.beacon.math.AABB;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec1i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.SSBO;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import java.util.Arrays;

public class OcclusionRenderer {
    private static final OcclusionProgram PROGRAM = new OcclusionProgram();

    private static final OcclusionProgram.CubeVertex[] CUBE_VERTICES = new OcclusionProgram.CubeVertex[]{
            new OcclusionProgram.CubeVertex(new Vec3f(1, 1, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 1, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 0, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 0, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 0, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 1, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 1, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 1, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 1, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 0, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 0, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 0, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 1, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 1, 0))
    };

    private static final VertexBuffer<OcclusionProgram.CubeVertex> CUBE_VBO = new VertexBuffer<>(OcclusionProgram.CubeVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STATIC_DRAW);

    static {
        CUBE_VBO.use(() -> {
            CUBE_VBO.data(CUBE_VERTICES);
        });
    }

    private final SSBO<StructArray<Vec1i>>                      ssbo      = new SSBO<>();
    private final VertexArray                                   va        = new VertexArray();
    private final VertexBuffer<OcclusionProgram.InstanceVertex> instances = new VertexBuffer<>(OcclusionProgram.InstanceVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STREAM_DRAW);
    private Query[] queries = new Query[0];

    public OcclusionRenderer() {
        va.use(() -> {
            OpenGL.use(CUBE_VBO, () -> {
                va.set(PROGRAM.pos, OcclusionProgram.CubeVertex.POSITION, CUBE_VBO, 0);
            });

            instances.use(() -> {
                va.set(PROGRAM.min, OcclusionProgram.InstanceVertex.MIN, instances, 1);
                va.set(PROGRAM.max, OcclusionProgram.InstanceVertex.MAX, instances, 1);
            });
        });
    }

    public void render(Matrix4f mvp, Query... queries) {
        this.queries = queries;

        OpenGL.depthTest(true);
        OpenGL.depthMask(false);
        OpenGL.colorMask(false);

        OpenGL.use(PROGRAM, va, ssbo, instances, () -> {
            instances.data(Arrays.stream(queries).map(query -> new OcclusionProgram.InstanceVertex(query.aabb.min(), query.aabb.max().subtract(query.aabb().min()))).toArray(OcclusionProgram.InstanceVertex[]::new));
            ssbo.data(OpenGL.Usage.STREAM_DRAW, new int[queries.length]); //TODO Optimize?
            PROGRAM.occlusion.set(ssbo);
            PROGRAM.mvp.set(mvp);

            OpenGL.drawArraysInstanced(OpenGL.Mode.TRIANGLE_STRIP, 0, CUBE_VERTICES.length, queries.length);
        });
    }

    public void read() {
        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
        ssbo.use(() -> {
            final int size = queries.length;
            final int[] result = new int[size];
            ssbo.get(0, size << 2).asIntBuffer().get(result);

            for (int i = 0; i < size; i++) {
                queries[i].consumer.accept(result[i] == 0);
            }
        });
    }

    public record Query(AABB aabb, BooleanConsumer consumer) {}
}
