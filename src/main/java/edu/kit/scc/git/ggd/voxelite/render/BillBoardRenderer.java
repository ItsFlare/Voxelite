package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.*;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Shader;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class BillBoardRenderer {

    private int size = 5;

    private Vec3f center = new Vec3f(0,10,0);

    private BillBoardProgram program = new BillBoardProgram();

    private static final BillBoardProgram.BillBoardVertex[] VERTICES = {
            new BillBoardProgram.BillBoardVertex(new Vec3f(0.5f,  0.5f, 0.0f)),
            new BillBoardProgram.BillBoardVertex(new Vec3f(0.5f, -0.5f, 0.0f)),
            new BillBoardProgram.BillBoardVertex(new Vec3f( -0.5f, -0.5f, 0.0f)),
            new BillBoardProgram.BillBoardVertex(new Vec3f( -0.5f,  0.5f, 0.0f)),
    };

    private static final int INDICES[] = {  // note that we start from 0!
            0, 1, 3,   // first triangle
            1, 2, 3    // second triangle
    };

    private final IBO ibo     = new IBO();

    private final VertexArray va      = new VertexArray();

    private final VertexBuffer<BillBoardProgram.BillBoardVertex> vb = new VertexBuffer<>(BillBoardProgram.BillBoardVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STATIC_DRAW);;

    public BillBoardRenderer() throws IOException {
        OpenGL.use(va, vb, ibo, () -> {
            vb.data(VERTICES);
            ibo.data(OpenGL.Usage.STATIC_DRAW, INDICES);
            va.set(program.pos, BillBoardProgram.BillBoardVertex.POSITION, vb, 0);

        });
    }

    public void render(Matrix4f matrix, Vec3f center) {
        OpenGL.depthTest(false);
        OpenGL.depthMask(false);

        OpenGL.use(program, va,  () -> {
            program.vp.set(matrix);
            program.center.set(center);

            OpenGL.drawIndexed(OpenGL.Mode.TRIANGLES, INDICES.length, OpenGL.Type.UNSIGNED_INT);
        });
    }

    public int getSize() {
        return size;
    }

    public Vec3f getCenter() {
        return center;
    }


}