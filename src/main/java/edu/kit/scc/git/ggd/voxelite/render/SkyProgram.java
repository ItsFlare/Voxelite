package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.ShaderLoader;
import net.durchholz.beacon.math.Matrix3f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.Vertex;
import net.durchholz.beacon.render.opengl.buffers.VertexAttribute;
import net.durchholz.beacon.render.opengl.buffers.VertexLayout;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class SkyProgram extends Program {

    public SkyProgram() {
        super(ShaderLoader.getSuite("sky"));
    }

    public final Attribute<Vec2f> ndc = attribute("ndc", OpenGL.Type.FLOAT, 2);

    public final Uniform<Vec2f> viewportResolution = uniVec2f("viewPortResolution");
    public final Uniform<Vec3f> sunPos = uniVec3f("sunPos");
    public final Uniform<Float> dayPercentage = uniFloat("dayPercentage");

    public final Uniform<Float> fov = uniFloat("fov");

    public final Uniform<Matrix3f> rotation = uniMatrix3f("rotation", true);

    record SkyVertex(Vec2f vec2f) implements Vertex {
        public static final VertexLayout<SkyProgram.SkyVertex> LAYOUT   = new VertexLayout<>(SkyProgram.SkyVertex.class);

        public static final VertexAttribute<Vec2f> POSITION = LAYOUT.vec2f(false);

        @Override
        public VertexLayout<?> getLayout() {
            return LAYOUT;
        }
    }
}

