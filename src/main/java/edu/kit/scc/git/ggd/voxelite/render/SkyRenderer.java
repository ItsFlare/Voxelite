package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.SimplexNoise;
import net.durchholz.beacon.math.*;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.IBO;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;
import net.durchholz.beacon.render.opengl.textures.CubemapTexture;
import net.durchholz.beacon.util.Image;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SkyRenderer {

    private final QuadRenderer   quadRenderer   = new QuadRenderer();
    private final SkyboxRenderer skyboxRenderer = new SkyboxRenderer(loadNightSkyBox());

    private static final SkyProgram.SkyVertex[] VERTICES = {
            new SkyProgram.SkyVertex(new Vec2f(-1.0f,  1.0f)),
            new SkyProgram.SkyVertex(new Vec2f(-1.0f, -1.0f)),
            new SkyProgram.SkyVertex( new Vec2f(1.0f,  1.0f)),
            new SkyProgram.SkyVertex( new Vec2f(1.0f, -1.0f)),
    };

    private static final int[] INDICES = {
            2, 0, 3,
            1, 0, 3,
    };



    private final SkyProgram                            program = new SkyProgram();
    private final VertexArray va      = new VertexArray();
    private final VertexBuffer<SkyProgram.SkyVertex> vb      = new VertexBuffer<>(SkyProgram.SkyVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STATIC_DRAW);

    private final IBO ibo     = new IBO();

    public SkyRenderer() throws IOException {

        OpenGL.use(va, vb, ibo,  () -> {
            vb.data(VERTICES);
            ibo.data(OpenGL.Usage.STATIC_DRAW, INDICES);
            va.set(program.ndc, SkyProgram.SkyVertex.POSITION, vb, 0);
        });
    }

    public void render(Vec3f color) {
        OpenGL.depthMask(false);
        OpenGL.depthTest(false);

        OpenGL.use(program, va,  () -> {
            program.color.set(color);

            OpenGL.drawIndexed(OpenGL.Mode.TRIANGLES, INDICES.length, OpenGL.Type.UNSIGNED_INT);
        });
    }

    public void renderSun(Matrix4f v, Matrix4f p) {
        Quaternion quaternion = Quaternion.ofAxisAngle(new Vec3f(Direction.NEG_X.getAxis()), getRotation()).normalized();
        final Matrix4f model = Matrix4f.identity();
        model.scale(0.1f);
        model.multiply(Matrix4f.rotation(quaternion));

        final Vec3f quadNormal = new Vec3f(Direction.POS_Z.getAxis());
        model.translate(quadNormal.rotate(quaternion));

        final Matrix4f view = v;
        final Matrix4f projection = p;
        view.multiply(model);
        projection.multiply(view);
        quadRenderer.render(projection, "glowstone.png");

    }

    private float getRotation() {
        return Main.getDayPercentage() * 360;
    }

    public void renderNightSkyBox(Matrix4f v, Matrix4f p) {
        final Matrix4f projection = p;
        projection.multiply(v);
        skyboxRenderer.render(projection);
    }

    private CubemapTexture loadNightSkyBox() throws IOException {
        final Image[] images = new Image[6];
        Noise noise = new SimplexNoise();
        int width = 256;
        int height = 256;

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                double noiseValue = noise.sample(new Vec2f(x,y));
                g2d.setColor(Color.BLACK);
                if (noiseValue > 0.9) {
                    g2d.setColor(Color.WHITE);
                }
                g2d.drawLine(x, y, x, y);
            }
        }
        g2d.dispose();
        ImageIO.write(bufferedImage, "png", new File("src/main/resources/textures/skybox/myimage.png"));
        for (int i = 0; i < 6; i++) {
            images[i] = new Image(Util.readResource("textures/skybox/myimage.png"));
        }
        return SkyboxRenderer.createCubemap(images);
    }

}
