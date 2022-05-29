package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.IBO;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;
import net.durchholz.beacon.render.opengl.textures.CubemapTexture;
import net.durchholz.beacon.util.Image;

import java.io.IOException;

import static net.durchholz.beacon.render.opengl.textures.GLTexture.*;
import static org.lwjgl.opengl.GL41.*;

public class SkyboxRenderer {

    private static final SkyboxProgram.SkyboxVertex[] VERTICES = {
            new SkyboxProgram.SkyboxVertex(new Vec3f(-1, -1, -1)),
            new SkyboxProgram.SkyboxVertex(new Vec3f(-1, 1, -1)),
            new SkyboxProgram.SkyboxVertex(new Vec3f(-1, -1, 1)),
            new SkyboxProgram.SkyboxVertex(new Vec3f(-1, 1, 1)),
            new SkyboxProgram.SkyboxVertex(new Vec3f(1, -1, -1)),
            new SkyboxProgram.SkyboxVertex(new Vec3f(1, 1, -1)),
            new SkyboxProgram.SkyboxVertex(new Vec3f(1, -1, 1)),
            new SkyboxProgram.SkyboxVertex(new Vec3f(1, 1, 1)),
    };


    private static final int[] INDICES = {
            1, 0, 4,
            4, 5, 1,

            2, 0, 1,
            1, 3, 2,

            4, 6, 7,
            7, 5, 4,

            2, 3, 7,
            7, 6, 2,

            1, 5, 7,
            7, 3, 1,

            0, 2, 4,
            4, 2, 6
    };

    private final SkyboxProgram                            program = new SkyboxProgram();
    private final VertexArray                              va      = new VertexArray();
    private final VertexBuffer<SkyboxProgram.SkyboxVertex> vb      = new VertexBuffer<>(SkyboxProgram.SkyboxVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STATIC_DRAW);
    private final IBO                                      ibo     = new IBO();

    private CubemapTexture skybox;

    public SkyboxRenderer(CubemapTexture skybox) throws IOException {
        this.skybox = skybox;

        OpenGL.use(va, vb, ibo, () -> {
            vb.data(VERTICES);
            ibo.data(OpenGL.Usage.STATIC_DRAW, INDICES);
            va.set(program.pos, SkyboxProgram.SkyboxVertex.POSITION, vb, 0);
        });
    }

    public void setSkybox(CubemapTexture skybox) {
        this.skybox = skybox;
    }

    public void render(Matrix4f matrix, float a) {
        OpenGL.depthTest(false);
        OpenGL.depthMask(false);
        OpenGL.blend(true);
        OpenGL.blendFunction(OpenGL.BlendFunction.SOURCE_ALPHA, OpenGL.BlendFunction.ONE_MINUS_SOURCE_ALPHA);

        OpenGL.use(program, va, skybox, () -> {
            program.mvp.set(matrix);
            program.alpha.set(a);

            program.skybox.bind(0, skybox);

            OpenGL.drawIndexed(OpenGL.Mode.TRIANGLES, INDICES.length, OpenGL.Type.UNSIGNED_INT);
        });

    }

    public static CubemapTexture createCubemap(Image[] images) {
        final CubemapTexture cubemapTexture = new CubemapTexture();

        cubemapTexture.use(() -> {
            cubemapTexture.image(images);

            for (TextureCoordinate coordinate : TextureCoordinate.values()) {
                cubemapTexture.wrapMode(coordinate, WrapMode.CLAMP_TO_EDGE);
            }

            cubemapTexture.minFilter(MinFilter.LINEAR);
            cubemapTexture.magFilter(MagFilter.NEAREST);
            cubemapTexture.generateMipmap();
        });

        return cubemapTexture;
    }
}
