package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.ShaderLoader;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.Vertex;
import net.durchholz.beacon.render.opengl.buffers.VertexAttribute;
import net.durchholz.beacon.render.opengl.buffers.VertexLayout;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class QuadProgram extends Program {

    public QuadProgram() {
        super(ShaderLoader.getSuite("quad"));
    }

    public final Attribute<Vec3f> pos = attribute("pos", OpenGL.Type.FLOAT, 3);
    public final Attribute<Vec2f> uv = attribute("uv", OpenGL.Type.FLOAT, 2);

    public final Uniform<Matrix4f> mvp = uniMatrix4f("mvp", true);
    public final Uniform<Vec2f> size = uniVec2f("size");
    public final Uniform<Vec2f> offset = uniVec2f("offset");

    public final Sampler sampler = sampler("sampler");

    record QuadVertex(Vec3f position, Vec2f uv) implements Vertex {
        public static final VertexLayout<QuadVertex> LAYOUT   = new VertexLayout<>(QuadVertex.class);
        public static final VertexAttribute<Vec3f>   POSITION = LAYOUT.vec3f(false);
        public static final VertexAttribute<Vec2f>   UV = LAYOUT.vec2f(false);

        @Override
        public VertexLayout<?> getLayout() {
            return LAYOUT;
        }
    }
}
