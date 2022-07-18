package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.Vertex;
import net.durchholz.beacon.render.opengl.buffers.VertexAttribute;
import net.durchholz.beacon.render.opengl.buffers.VertexLayout;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Shader;

public abstract class ScreenProgram extends Program {
    public final Attribute<Vec2f> pos = attribute("pos", OpenGL.Type.FLOAT, 2);

    public ScreenProgram(Shader... shaders) {
        super(shaders);
    }

    public record QuadVertex(Vec2f pos) implements Vertex {
        public static final VertexLayout<CompositeProgram.QuadVertex> LAYOUT = new VertexLayout<>(QuadVertex.class);
        public static final VertexAttribute<Vec2f> POSITION = LAYOUT.vec2f(false);

        @Override
        public VertexLayout<CompositeProgram.QuadVertex> getLayout() {
            return LAYOUT;
        }
    }
}
