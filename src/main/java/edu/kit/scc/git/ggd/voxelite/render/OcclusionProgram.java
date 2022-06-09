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

public class OcclusionProgram extends Program {

    public final Attribute<Vec3f> pos = attribute("pos", OpenGL.Type.FLOAT, 3);
    public final Attribute<Vec3f> min = attribute("min", OpenGL.Type.FLOAT, 3);
    public final Attribute<Vec3f> max = attribute("max", OpenGL.Type.FLOAT, 3);

    public final Uniform<Matrix4f> mvp       = uniMatrix4f("mvp", true);
    public final StorageBuffer     occlusion = storageBuffer("occlusionBuffer", 0);

    public OcclusionProgram() {
        super(Shader.vertex(Util.readShaderResource("occlusion.vs")), Shader.fragment(Util.readShaderResource("occlusion.fs")));
    }

    public record CubeVertex(Vec3f position) implements Vertex {
        public static final VertexLayout<CubeVertex> LAYOUT   = new VertexLayout<>(CubeVertex.class);
        public static final VertexAttribute<Vec3f>   POSITION = LAYOUT.vec3f(false);

        @Override
        public VertexLayout<CubeVertex> getLayout() {
            return LAYOUT;
        }
    }

    public record InstanceVertex(Vec3f min, Vec3f max) implements Vertex {
        public static final VertexLayout<InstanceVertex> LAYOUT = new VertexLayout<>(InstanceVertex.class);
        public static final VertexAttribute<Vec3f>       MIN    = LAYOUT.vec3f(false);
        public static final VertexAttribute<Vec3f>       MAX    = LAYOUT.vec3f(false);

        @Override
        public VertexLayout<InstanceVertex> getLayout() {
            return LAYOUT;
        }
    }
}
