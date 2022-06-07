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
import java.util.concurrent.ThreadLocalRandom;

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

    public void render(Vec3f color, Vec3f direction, Vec2f viewportRes, float dayPercentage, float fov, Matrix3f rotation) {
        OpenGL.depthMask(false);
        OpenGL.depthTest(false);

        Quaternion quaternion = Quaternion.ofAxisAngle(new Vec3f(Direction.NEG_X.getAxis()), getRotation()).normalized();
        final Matrix4f model = Matrix4f.identity();
        model.scale(0.1f);
        model.multiply(Matrix4f.rotation(quaternion));

        final Vec3f quadNormal = new Vec3f(Direction.POS_Z.getAxis());
        model.translate(quadNormal.rotate(quaternion));

        Matrix4f matrix4f = new Matrix4f(0.0f, 0,0,0,0.0f,0,0,0,0,0,0,0,1,0,0,0);
        model.multiply(matrix4f);

        Vec3f sunPos = new Vec3f(model.get(0,0), model.get(1,0),model.get(2,0));
        //System.out.println(sunPos);
        //System.out.println(direction);


        OpenGL.use(program, va,  () -> {
            program.sunPos.set(sunPos);
            program.color.set(color);
            program.direction.set(direction);
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
        quadRenderer.render(projection, "glowstone.png");

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

    private CubemapTexture loadNightSkyBox() throws IOException {
        final Image[] images = new Image[6];
        Noise noise = new SimplexNoise();
        int size = 512;
        int center = size / 2;

        BufferedImage bufferedImage = new BufferedImage(size , size , BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        for(int x = 0; x < size; x++) {
            for(int y = 0; y < size; y++) {
                double noiseValue = noise.sample(new Vec2f(x,y));
                double distance = getDistance(center, center, x, y);

                if (noiseValue > 0.9 && distance <= center) {
                    g2d.setColor(new Color(1f,1f,1f, 1 - ThreadLocalRandom.current().nextFloat(0.7f)));
                } else {
                    g2d.setColor(new Color(0,0,0,0));
                }
                g2d.drawLine(x, y, x, y);
            }
        }
        g2d.dispose();
        ImageIO.write(bufferedImage, "png", new File("src/main/resources/textures/skybox/nightsky.png"));
        for (int i = 0; i < 6; i++) {
            images[i] = new Image(Util.readResource("textures/skybox/nightsky.png"));
        }
        return SkyboxRenderer.createCubemap(images);
    }

    private double getDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1 - y2, 2));
    }

}