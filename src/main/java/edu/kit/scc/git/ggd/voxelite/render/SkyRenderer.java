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
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.util.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class SkyRenderer {

    private final QuadRenderer   quadRenderer   = new QuadRenderer();
    private final SkyboxRenderer skyboxRenderer = new SkyboxRenderer(loadNightSkyBox());

    private final Texture2D sunTexture = new Texture2D();

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
        Image image = new Image(Util.readResource("textures/skybox/sun.png"));

        OpenGL.use(va, vb, ibo, sunTexture,  () -> {
            vb.data(VERTICES);
            ibo.data(OpenGL.Usage.STATIC_DRAW, INDICES);
            va.set(program.ndc, SkyProgram.SkyVertex.POSITION, vb, 0);

            sunTexture.image(image);
            sunTexture.magFilter(GLTexture.MagFilter.NEAREST);
            sunTexture.minFilter(GLTexture.MinFilter.NEAREST);
        });
    }

    public void render(Vec3f color, Vec3f direction, Vec2f viewportRes, float dayPercentage, float fov, Matrix3f rotation) {
        OpenGL.depthMask(false);
        OpenGL.depthTest(false);

        Quaternion quaternion = Quaternion.ofAxisAngle(new Vec3f(Direction.NEG_X.getAxis()), getRotation()).normalized();
        final Matrix4f model = Matrix4f.identity();
        model.scale(0.1f);
        model.multiply(Matrix4f.rotation(quaternion));

        final Vec3f quadNormal = new Vec3f(Direction.POS_Z.getAxis());
        final Vec3f position = quadNormal.rotate(quaternion);
        model.translate(position);

        OpenGL.use(program, va,  () -> {
            program.sunPos.set(position);
            program.viewportResolution.set(viewportRes);
            program.dayPercentage.set(dayPercentage);
            program.fov.set(fov);
            program.rotation.set(rotation);


            OpenGL.drawIndexed(OpenGL.Mode.TRIANGLES, INDICES.length, OpenGL.Type.UNSIGNED_INT);
        });
    }

    public void renderSun(Matrix4f v, Matrix4f p) {
        OpenGL.blend(false);

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
        quadRenderer.render(projection, sunTexture,new Vec2f(), new Vec2f(1));

    }

    private float getRotation() {
        return Main.getDayPercentage() * 360;
    }

    public void renderNightSkyBox(Matrix4f v, Matrix4f p, float a) {
        Quaternion quaternion = Quaternion.ofAxisAngle(new Vec3f(Direction.NEG_X.getAxis()), getRotation()).normalized();
        final Matrix4f model = Matrix4f.identity();
        model.multiply(Matrix4f.rotation(quaternion));

        v.multiply(model);
        p.multiply(v);
        skyboxRenderer.render(p, a);
    }

    private CubemapTexture loadNightSkyBox() {
        Noise noise = new SimplexNoise();
        int size = 512;
        int center = size / 2;

        BufferedImage bufferedImage = new BufferedImage(size , size , BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        for(int x = 0; x < size; x++) {
            for(int y = 0; y < size; y++) {
                Vec2f vec = new Vec2f(x,y);
                double noiseValue = noise.sample(vec);
                double distance = vec.subtract(center).magnitude() / (double) center;

                g2d.setColor(new Color(1f,1f,1f, 1 - ThreadLocalRandom.current().nextFloat(0.7f)));
                if (distance <= 0.9) {
                    if (noiseValue > 0.9) {
                        g2d.drawLine(x, y, x, y);
                    }
                } else {
                    if (noiseValue - ((distance - 0.9) / 15) > 0.9) {
                        g2d.drawLine(x, y, x, y);
                    }
                }
            }
        }
        //ImageIO.write(bufferedImage, "png", new File("src/main/resources/textures/skybox/nightsky.png"));
        g2d.dispose();

        final Image[] images = new Image[6];
        Arrays.fill(images, new Image(bufferedImage));

        return SkyboxRenderer.createCubemap(images);
    }
}