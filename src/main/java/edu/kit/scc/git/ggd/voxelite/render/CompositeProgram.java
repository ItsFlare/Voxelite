package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.Vertex;
import net.durchholz.beacon.render.opengl.buffers.VertexAttribute;
import net.durchholz.beacon.render.opengl.buffers.VertexLayout;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class CompositeProgram extends Program {

    public final Attribute<Vec2f> pos = attribute("pos", OpenGL.Type.FLOAT, 2);

    public final Uniform<Vec2f> planes      = uniVec2f("planes");
    public final Sampler        opaque      = sampler("opaque");
    public final Sampler        transparent = sampler("transparent");
    public final Sampler        normal      = sampler("normal");
    public final Sampler        mer         = sampler("mer");
    public final Sampler        depth       = sampler("depth");
    public final Uniform<Matrix4f> projection = uniMatrix4f("projection", true);
//    public final Sampler shadowMap = sampler("shadowMap");

    public CompositeProgram() {
        super(Util.loadShaders("composite"));
    }

    public record QuadVertex(Vec2f pos) implements Vertex {
        public static final VertexLayout<QuadVertex> LAYOUT   = new VertexLayout<>(QuadVertex.class);
        public static final VertexAttribute<Vec2f>   POSITION = LAYOUT.vec2f(false);

        @Override
        public VertexLayout<QuadVertex> getLayout() {
            return LAYOUT;
        }
    }
}
