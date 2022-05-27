package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.Vertex;
import net.durchholz.beacon.render.opengl.buffers.VertexAttribute;
import net.durchholz.beacon.render.opengl.buffers.VertexLayout;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

import java.io.IOException;

public class SkyProgram extends Program {

    public SkyProgram() throws IOException {
        super(Shader.vertex(Util.readShaderResource("sky.vs")), Shader.fragment(Util.readShaderResource("sky.fs")));
    }

    public final Attribute<Vec2f> ndc = attribute("ndc", OpenGL.Type.FLOAT, 2);

    public final Uniform<Vec3f> color = uniVec3f("color");

    record SkyVertex(Vec2f vec2f) implements Vertex {
        public static final VertexLayout<SkyProgram.SkyVertex> LAYOUT   = new VertexLayout<>(SkyProgram.SkyVertex.class);

        public static final VertexAttribute<Vec2f> POSITION = LAYOUT.vec2f(false);

        @Override
        public VertexLayout<?> getLayout() {
            return LAYOUT;
        }
    }
}

