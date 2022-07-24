package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.event.Listener;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.FBO;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.window.Viewport;
import net.durchholz.beacon.window.event.ViewportResizeEvent;

import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT4;

public class BloomBlurRenderer extends ScreenRenderer {

    public static final BlurProgram PROGRAM = new BlurProgram();

    private final FBO       fbo     = new FBO();
    private final Texture2D texture = new Texture2D();

    public BloomBlurRenderer() {
        super(PROGRAM);

        texture.use(() -> {
            texture.minFilter(GLTexture.MinFilter.LINEAR);
            texture.magFilter(GLTexture.MagFilter.LINEAR);
            texture.wrapMode(GLTexture.TextureCoordinate.S, GLTexture.WrapMode.CLAMP_TO_EDGE);
            texture.wrapMode(GLTexture.TextureCoordinate.T, GLTexture.WrapMode.CLAMP_TO_EDGE);
            texture.allocate(1, 1, GLTexture.SizedFormat.RGB_8);
        });

        fbo.use(() -> {
            fbo.color(0, texture);
        });

        EventType.addListener(this);
    }

    @Listener
    private void onResize(ViewportResizeEvent event) {
        final Viewport viewport = event.viewport();
        if(!viewport.isZero()) texture.use(() -> texture.allocate(viewport.width(), viewport.height(), GLTexture.SizedFormat.RGB_8));
    }

    public void render(GeometryBuffer gBuffer, int iterations) {
        OpenGL.use(PROGRAM, va, () -> {
            OpenGL.depthTest(false);
            OpenGL.blend(false);

            for (int i = 0; i < iterations; i++) {
                blurDirection(gBuffer, new Vec2f(1, 0));
                blurDirection(gBuffer, new Vec2f(0, 1));
            }
        });
    }

    private void blurDirection(GeometryBuffer gBuffer, Vec2f direction) {
        fbo.use(() -> {
            OpenGL.setDrawBuffers(GL_COLOR_ATTACHMENT0);
            PROGRAM.sampler.bind(0, gBuffer.bloom());
            PROGRAM.direction.set(direction);
            drawScreen();
        });

        gBuffer.use(() -> {
            OpenGL.setDrawBuffers(GL_COLOR_ATTACHMENT4);
            PROGRAM.sampler.bind(0, texture);
            PROGRAM.direction.set(direction);
            drawScreen();
        });
    }
}
