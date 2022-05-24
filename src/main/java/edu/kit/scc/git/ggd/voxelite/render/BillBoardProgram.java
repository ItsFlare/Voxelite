package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.Vertex;
import net.durchholz.beacon.render.opengl.buffers.VertexAttribute;
import net.durchholz.beacon.render.opengl.buffers.VertexLayout;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

import java.io.IOException;

public class BillBoardProgram extends Program {

    public BillBoardProgram() throws IOException {
        super(Shader.vertex(Util.readShaderResource("sun.vs")), Shader.fragment(Util.readShaderResource("sun.fs")));
    }

    public final Attribute<Vec3f> pos = attribute("pos", OpenGL.Type.FLOAT, 3);

    public final Uniform<Matrix4f> vp = uniMatrix4f("vp", true);
    public final Uniform<Vec3f> center = uniVec3f("center");

    //public final Sampler skybox = sampler("skybox");

    record BillBoardVertex(Vec3f position) implements Vertex {
        public static final VertexLayout<BillBoardProgram.BillBoardVertex> LAYOUT = new VertexLayout<>(BillBoardProgram.BillBoardVertex.class);
        public static final VertexAttribute<Vec3f> POSITION = LAYOUT.vec3f(false);

        @Override
        public VertexLayout<?> getLayout() {
            return LAYOUT;
        }
    }
}
