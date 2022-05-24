package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec4f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.VertexAttribute;
import net.durchholz.beacon.render.opengl.buffers.VertexLayout;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Shader;

public class SpriteProgram extends Program {

    public final Attribute<Vec2f> position = attribute("aPos", OpenGL.Type.FLOAT, 2);
    public final Attribute<Vec2f> texture = attribute("aTex", OpenGL.Type.FLOAT, 2);
    public final Attribute<Vec4f> tint = attribute("aTint", OpenGL.Type.FLOAT, 4);

    public final Sampler sampler = sampler("sampler");

    public SpriteProgram(Shader... shaders) {
        super(shaders);
    }

    public record Vertex(Vec2f position, Vec2f texture, Vec4f tint) implements net.durchholz.beacon.render.opengl.buffers.Vertex {
        public static final VertexLayout<Vertex> LAYOUT = new VertexLayout<>(Vertex.class);
        public static final VertexAttribute<Vec2f> POSITION = LAYOUT.vec2f(false);
        public static final VertexAttribute<Vec2f> TEXTURE = LAYOUT.vec2f(false);
        public static final VertexAttribute<Vec4f> TINT = LAYOUT.vec4f(false);

        @Override
        public VertexLayout<?> getLayout() {
            return LAYOUT;
        }
    }
}
