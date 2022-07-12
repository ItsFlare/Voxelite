package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.ShaderLoader;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec4f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.Vertex;
import net.durchholz.beacon.render.opengl.buffers.VertexAttribute;
import net.durchholz.beacon.render.opengl.buffers.VertexLayout;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class LineProgram extends Program {

    public final Attribute<Vec3f> pos   = attribute("pos", OpenGL.Type.FLOAT, 3);
    public final Attribute<Vec4f> color = attribute("color", OpenGL.Type.FLOAT, 4);

    public final Uniform<Matrix4f> mvp = uniMatrix4f("mvp", true);

    public LineProgram() {
        super(ShaderLoader.getSuite("line"));
    }

    record LineVertex(Vec3f position, Vec4f color) implements Vertex {
        public static final VertexLayout<LineProgram.LineVertex> LAYOUT   = new VertexLayout<>(LineProgram.LineVertex.class);
        public static final VertexAttribute<Vec3f>               POSITION = LAYOUT.vec3f(false);
        public static final VertexAttribute<Vec4f>               COLOR    = LAYOUT.vec4f(false);

        @Override
        public VertexLayout<LineVertex> getLayout() {
            return LAYOUT;
        }
    }
}
