package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.ShaderLoader;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.Vertex;
import net.durchholz.beacon.render.opengl.buffers.VertexAttribute;
import net.durchholz.beacon.render.opengl.buffers.VertexLayout;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Uniform;

import java.io.IOException;

public class SkyboxProgram extends Program {

    public SkyboxProgram() throws IOException {
        super(ShaderLoader.getSuite("skybox"));
    }

    public final Attribute<Vec3f> pos = attribute("pos", OpenGL.Type.FLOAT, 3);

    public final Uniform<Matrix4f> mvp = uniMatrix4f("mvp", true);
    public final Sampler skybox = sampler("skybox");

    public final Uniform<Float> alpha = uniFloat("alpha");

    record SkyboxVertex(Vec3f position) implements Vertex {
        public static final VertexLayout<SkyboxVertex> LAYOUT = new VertexLayout<>(SkyboxVertex.class);
        public static final VertexAttribute<Vec3f> POSITION = LAYOUT.vec3f(false);

        @Override
        public VertexLayout<?> getLayout() {
            return LAYOUT;
        }
    }
}
