package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.SimplexNoise;
import net.durchholz.beacon.math.*;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;
import net.durchholz.beacon.render.opengl.textures.CubemapTexture;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.util.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class SkyRenderer {

    private final QuadRenderer   quadRenderer   = new QuadRenderer();
    private final SkyboxRenderer skyboxRenderer = new SkyboxRenderer(loadNightSkyBox());

    private final Texture2D sunTexture  = new Texture2D();
    private final Texture2D moonTexture = new Texture2D();

    private static final SkyProgram.SkyVertex[] VERTICES = {
            new SkyProgram.SkyVertex(new Vec2f(1, 1)),
            new SkyProgram.SkyVertex(new Vec2f(-1, 1)),
            new SkyProgram.SkyVertex(new Vec2f(1, -1)),
            new SkyProgram.SkyVertex(new Vec2f(-1, -1)),
    };

    private final SkyProgram                         program = new SkyProgram();
    private final VertexArray                        va      = new VertexArray();
    private final VertexBuffer<SkyProgram.SkyVertex> vb      = new VertexBuffer<>(SkyProgram.SkyVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STATIC_DRAW);

    public SkyRenderer() throws IOException {

        OpenGL.use(va, vb, () -> {
            vb.data(VERTICES);
            va.set(program.ndc, SkyProgram.SkyVertex.POSITION, vb, 0);
        });

        Image sun = new Image(Util.readResource("textures/skybox/sun.png"));
        sunTexture.use(() -> {
            sunTexture.image(sun);
            sunTexture.magFilter(GLTexture.MagFilter.NEAREST);
            sunTexture.minFilter(GLTexture.MinFilter.NEAREST);
        });

        Image moon = new Image(Util.readResource("textures/skybox/moon.png"));
        moonTexture.use(() -> {
            moonTexture.image(moon);
            moonTexture.magFilter(GLTexture.MagFilter.NEAREST);
            moonTexture.minFilter(GLTexture.MinFilter.NEAREST);
        });
    }

    public void render(Vec2f viewportRes, float dayPercentage, float fov, Matrix3f rotation) {
        OpenGL.colorMask(true);
        OpenGL.depthMask(false);
        OpenGL.depthTest(false);
        OpenGL.cull(false);

        Quaternion quaternion = Quaternion.ofAxisAngle(new Vec3f(Direction.NEG_X.getAxis()), getRotation()).normalized();
        final Matrix4f model = Matrix4f.identity();
        model.scale(0.1f);
        model.multiply(Matrix4f.rotation(quaternion));

        final Vec3f quadNormal = new Vec3f(Direction.POS_Z.getAxis());
        final Vec3f position = quadNormal.rotate(quaternion);
        model.translate(position);
        System.out.println(position);

        OpenGL.use(program, va, () -> {
            program.sunPos.set(position);
            program.viewportResolution.set(viewportRes);
            program.dayPercentage.set(dayPercentage);
            program.fov.set(fov);
            program.rotation.set(rotation);

            OpenGL.drawArrays(OpenGL.Mode.TRIANGLE_STRIP, 0, VERTICES.length);
        });
    }

    public void renderPlanets(Matrix4f vp) {
        Quaternion quaternion;

        quaternion = Quaternion.ofAxisAngle(new Vec3f(Direction.NEG_X.getAxis()), getRotation()).normalized();
        renderPlanetQuad(vp, quaternion, 0.1f, sunTexture);
        quaternion = Quaternion.ofAxisAngle(new Vec3f(Direction.NEG_X.getAxis()), getRotation() + 180).normalized();
        renderPlanetQuad(vp, quaternion, 0.1f, moonTexture);
    }

    public void renderPlanetQuad(Matrix4f vp, Quaternion rotation, float scale, Texture2D texture) {
        OpenGL.blend(false);
        vp = vp.clone();

        final Matrix4f model = Matrix4f.identity();
        model.scale(scale);
        model.multiply(Matrix4f.rotation(rotation));

        final Vec3f quadNormal = new Vec3f(Direction.POS_Z.getAxis());
        model.translate(quadNormal.rotate(rotation));

        vp.multiply(model);

        quadRenderer.render(vp, texture, new Vec2f(), new Vec2f(1));
    }

    private float getRotation() {
        return Main.getDayPercentage() * 360;
    }

    public void renderNightSkyBox(Matrix4f vp, float a) {
        Quaternion quaternion = Quaternion.ofAxisAngle(new Vec3f(Direction.NEG_X.getAxis()), getRotation()).normalized();
        final Matrix4f model = Matrix4f.identity();
        model.multiply(Matrix4f.rotation(quaternion));
        vp = vp.clone();
        vp.multiply(model);

        skyboxRenderer.render(vp, a);
    }

    private CubemapTexture loadNightSkyBox() {
        Noise noise = new SimplexNoise();
        int size = 512;
        int center = size / 2;

        BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Vec2f vec = new Vec2f(x, y);
                double noiseValue = noise.sample(vec);
                double distance = vec.subtract(center).magnitude() / (double) center;
                final double threshold = 0.95;
                final double radius = 0.9;

                g2d.setColor(new Color(1f, 1f, 1f, 1 - ThreadLocalRandom.current().nextFloat(0.7f)));
                if (distance <= radius) {
                    if (noiseValue > threshold) {
                        g2d.drawLine(x, y, x, y);
                    }
                } else {
                    if (noiseValue - ((distance - radius) / 10) > threshold) {
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