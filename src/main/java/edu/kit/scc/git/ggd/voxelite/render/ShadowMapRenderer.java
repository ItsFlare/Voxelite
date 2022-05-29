package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Frustum;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.math.AABB;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.FBO;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.window.Viewport;

import static org.lwjgl.opengl.GL43.GL_NONE;

public class ShadowMapRenderer {

    public static final ChunkShadowProgram PROGRAM = new ChunkShadowProgram(Shader.vertex(Util.readShaderResource("chunk_opaque_shadow.vs")), Shader.fragment(Util.readShaderResource("default.fs")));
    private final FBO                      fbo     = new FBO();
    private final Texture2D texture = new Texture2D();
    private final int size;

    public ShadowMapRenderer(int size) {
        this.size = size;
        OpenGL.use(fbo, texture, () -> {
            texture.allocate(size, size, GLTexture.BaseFormat.DEPTH_COMPONENT);
            texture.minFilter(GLTexture.MinFilter.NEAREST);
            texture.magFilter(GLTexture.MagFilter.NEAREST);
            OpenGL.setDrawBuffers(GL_NONE);
//            glDrawBuffer(GL_NONE);
//            glReadBuffer(GL_NONE);
            fbo.depth(texture, 0);
        });
    }

    public static Matrix4f lightTransform(Frustum frustum, Vec3f lightDirection) {
        var targetPos = Main.INSTANCE.getRenderer().getCamera().getPosition();

        final Matrix4f projection = Matrix4f.orthographic(new AABB(new Vec3f(-100), new Vec3f(100)));
        final Matrix4f view = Matrix4f.look(targetPos.subtract(lightDirection), targetPos);
        projection.multiply(view);

        return projection;
    }

    public void render(Frustum frustum, Vec3f lightDirection) {
        OpenGL.setViewport(new Viewport(size, size));

        final int visibilityBitset = ChunkProgram.Slice.directionCull(lightDirection.scale(-1000), new Vec3i(0));

        OpenGL.use(PROGRAM, fbo, () -> {
            OpenGL.clearAll();

            for (RenderChunk renderChunk : Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunks()) {
                PROGRAM.mvp.set(lightTransform(frustum, lightDirection));
                PROGRAM.chunk.set(Chunk.toWorldPosition(renderChunk.getChunk().getPosition()));

                renderChunk.renderShadow(RenderType.OPAQUE, visibilityBitset);
            }

        });

        OpenGL.setViewport(Main.INSTANCE.getWindow().getViewport());
    }

    public Texture2D getTexture() {
        return texture;
    }
}
