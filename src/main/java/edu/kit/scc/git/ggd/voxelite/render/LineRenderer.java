package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Frustum;
import net.durchholz.beacon.math.AABB;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec4f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;

import java.util.ArrayList;
import java.util.Arrays;

public class LineRenderer {

    private static final LineProgram PROGRAM = new LineProgram();

    private final VertexArray                          va = new VertexArray();
    private final VertexBuffer<LineProgram.LineVertex> vb = new VertexBuffer<>(LineProgram.LineVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STREAM_DRAW);

    public LineRenderer() {
        OpenGL.use(va, vb, () -> {
            va.set(PROGRAM.pos, LineProgram.LineVertex.POSITION, vb, 0);
            va.set(PROGRAM.color, LineProgram.LineVertex.COLOR, vb, 0);
        });
    }

    public void render(Matrix4f mvp, Vec4f color, Vec3f... points) {
        render(OpenGL.Mode.LINES, mvp, Arrays.stream(points).map(vec3f -> new LineProgram.LineVertex(vec3f, color)).toArray(LineProgram.LineVertex[]::new));
    }

    public void renderStrip(Matrix4f mvp, Vec4f color, Vec3f... points) {
        render(OpenGL.Mode.LINE_STRIP, mvp, Arrays.stream(points).map(vec3f -> new LineProgram.LineVertex(vec3f, color)).toArray(LineProgram.LineVertex[]::new));
    }

    public void render(Matrix4f mvp, Vec4f color, Frustum frustum) {
        renderBoxCorners(mvp, color, frustum.corners());
    }

    public void render(Matrix4f mvp, Vec4f color, AABB aabb) {
        renderBoxCorners(mvp, color, aabb.corners());
    }

    public void render(Matrix4f mvp, LineProgram.LineVertex... vertices) {
        render(OpenGL.Mode.LINES, mvp, vertices);
    }

    public void render(OpenGL.Mode mode, Matrix4f mvp, LineProgram.LineVertex... vertices) {
        OpenGL.colorMask(true);
        OpenGL.depthMask(false);
        OpenGL.depthTest(true);
        OpenGL.blend(true);

        OpenGL.use(PROGRAM, va, vb, () -> {
            PROGRAM.mvp.set(mvp);

            vb.data(vertices);

            OpenGL.drawArrays(mode, 0, vertices.length);
        });
    }


    private void renderBoxCorners(Matrix4f mvp, Vec4f color, Vec3f[] corners) {
        var result = new ArrayList<LineProgram.LineVertex>();

        //Depth
        for (int i = 0; i < 4; i++) {
            result.add(new LineProgram.LineVertex(corners[i], color));
            result.add(new LineProgram.LineVertex(corners[i + 4], color));
        }

        //Near plane
        for (int i = 0; i < 4; i++) {
            result.add(new LineProgram.LineVertex(corners[i], color));
            result.add(new LineProgram.LineVertex(corners[(i + 1) % 4], color));
        }

        //Far plane
        for (int i = 0; i < 4; i++) {
            result.add(new LineProgram.LineVertex(corners[i + 4], color));
            result.add(new LineProgram.LineVertex(corners[((i + 1) % 4) + 4], color));
        }

        render(mvp, result.toArray(LineProgram.LineVertex[]::new));
    }
}
