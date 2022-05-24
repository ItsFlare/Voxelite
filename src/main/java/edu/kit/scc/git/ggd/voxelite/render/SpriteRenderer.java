package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec4f;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.util.Image;

import static net.durchholz.beacon.render.opengl.OpenGL.*;

public class SpriteRenderer {

    public static final SpriteProgram PROGRAM  = new SpriteProgram(Shader.vertex(Util.readShaderResource("sprite.vs")), Shader.fragment(Util.readShaderResource("sprite.fs")));
    private static final Vec2f[]      VERTICES = new Vec2f[] {
            new Vec2f(0, 1),
            new Vec2f(0, 0),
            new Vec2f(1, 1),
            new Vec2f(1, 0)
    };

    private final Texture2D texture = new Texture2D();
    private final VertexArray vertexArray = new VertexArray();
    private final VertexBuffer<SpriteProgram.Vertex> vertexBuffer = new VertexBuffer<>(SpriteProgram.Vertex.LAYOUT, BufferLayout.INTERLEAVED, Usage.DYNAMIC_DRAW);
    private final int width, height;

    public SpriteRenderer(Image image) {
        this.width = image.width();
        this.height = image.height();

        texture.use(() -> {
            texture.magFilter(GLTexture.MagFilter.NEAREST);
            texture.minFilter(GLTexture.MinFilter.LINEAR);
            texture.image(image);
        });

        use(vertexArray, vertexBuffer, () -> {
            vertexArray.set(PROGRAM.position, SpriteProgram.Vertex.POSITION, vertexBuffer, 0);
            vertexArray.set(PROGRAM.texture, SpriteProgram.Vertex.TEXTURE, vertexBuffer, 0);
            vertexArray.set(PROGRAM.tint, SpriteProgram.Vertex.TINT, vertexBuffer, 0);
        });
    }

    public void update(Vec2f position, float scale, Vec4f tint, boolean viewportCenter, boolean imageCenter) {
        var viewport = Main.INSTANCE.getWindow().getViewport();

        SpriteProgram.Vertex[] v = new SpriteProgram.Vertex[VERTICES.length];

        for (int i = 0; i < VERTICES.length; i++) {
            Vec2f vertex = VERTICES[i];

            vertex = vertex.scale(new Vec2f(width, height)); //Scale by image size
            if (imageCenter) vertex = vertex.subtract(new Vec2f(width / 2f, height / 2f)); //Center image
            vertex = vertex.scale(scale); //Scale by factor
            vertex = vertex.divide(new Vec2f(viewport.width(), viewport.height())); //Normalize
            vertex = vertex.add(position);
            if (!viewportCenter) vertex = vertex.subtract(1); //Relative to clip space origin

            v[i] = new SpriteProgram.Vertex(vertex, VERTICES[i], tint);
        }

        vertexBuffer.use(() -> {
            vertexBuffer.data(v);
        });
    }

    public void render() {
        blend(true);
        blendEquation(BlendEquation.ADD);
        blendFunction(BlendFunction.ONE, BlendFunction.ONE_MINUS_SOURCE_ALPHA);

        activeTextureUnit(0);
        use(vertexArray, texture, () -> {
            drawArrays(Mode.TRIANGLE_STRIP, 0, VERTICES.length);
        });

        blend(false);
    }
}
