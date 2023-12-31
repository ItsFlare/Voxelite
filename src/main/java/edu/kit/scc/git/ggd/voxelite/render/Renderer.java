package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.ui.UserInterface;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.FBM;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.SimplexNoise;
import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.event.Listener;
import net.durchholz.beacon.math.Matrix3f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.textures.CubemapTexture;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.util.Image;
import net.durchholz.beacon.window.Viewport;
import net.durchholz.beacon.window.Window;
import net.durchholz.beacon.window.event.ViewportResizeEvent;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.sin;
import static org.lwjgl.opengl.GL30.*;

public class Renderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Renderer.class);

    private final Camera         camera;
    private final UserInterface  userInterface;
    private final WorldRenderer  worldRenderer;
    private final SkyRenderer    skyRenderer       = new SkyRenderer();
    private final SpriteRenderer crosshairRenderer = new SpriteRenderer(new Image(Util.readResource("textures/crosshair.png")));
    private final GeometryBuffer gBuffer           = new GeometryBuffer(1, 1);
    private final Texture2D      noiseTexture      = new Texture2D();

    public boolean renderUI     = true;
    public boolean renderSkybox = true;
    public boolean renderWorld  = true;
    public boolean wireframe    = false;

    private int frame;

    public Renderer(Window window) throws IOException {
        this.camera = new Camera(window);
        this.userInterface = new UserInterface();
        this.worldRenderer = new WorldRenderer();
        gBuffer.allocate(window.getViewport().width(), window.getViewport().height());

        noiseTexture.use(() -> {
            noiseTexture.image(generateNoiseImage(new Vec2i(256)));
            noiseTexture.magFilter(GLTexture.MagFilter.NEAREST);
            noiseTexture.minFilter(GLTexture.MinFilter.NEAREST);
            noiseTexture.wrapMode(GLTexture.TextureCoordinate.S, GLTexture.WrapMode.MIRRORED_REPEAT);
            noiseTexture.wrapMode(GLTexture.TextureCoordinate.T, GLTexture.WrapMode.MIRRORED_REPEAT);
        });

        EventType.addListener(this);
    }

    public void init() {
        userInterface.init();
    }

    public void shutdown() {
        userInterface.shutdown();
    }

    public Camera getCamera() {
        return camera;
    }

    public WorldRenderer getWorldRenderer() {
        return worldRenderer;
    }


    public void render() {
        OpenGL.polygonMode(OpenGL.Face.BOTH, wireframe ? OpenGL.PolygonMode.LINE : OpenGL.PolygonMode.FILL);

        gBuffer.use(() -> {
            OpenGL.setDrawBuffers(GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4);
            glClearDepth(1); //Background is at infinity (required for sky reflection)
            OpenGL.clearAll();
        });

        if (renderSkybox) renderSky();
        if (renderWorld) renderWorld();

        if (wireframe) OpenGL.polygonMode(OpenGL.Face.BOTH, OpenGL.PolygonMode.FILL);
        if (renderUI) {
            renderCrosshair();
            renderUserInterface();
        }

        frame++;
    }

    public void tick() {
        worldRenderer.tick();
        userInterface.tick();
    }

    @Listener
    private void onResize(ViewportResizeEvent event) {
        final Viewport viewport = event.viewport();
        if(!viewport.isZero()) gBuffer.allocate(viewport.width(), viewport.height());
    }

    private void renderSky() {
        float dayPercentage = Util.clamp((float) sin(2 * Math.PI * Main.getDayPercentage()) + 0.75f, 0, 1);

        var projection = camera.projection();
        projection.multiply(camera.view(false, true));
        final Vec2f viewportRes = new Vec2f(Main.INSTANCE.getWindow().getViewport().width(), Main.INSTANCE.getWindow().getViewport().height());

        gBuffer.use(() -> {
            OpenGL.setDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
            skyRenderer.render(viewportRes, dayPercentage, camera.getFOV(), Matrix3f.rotation(camera.getRotation()));
            skyRenderer.renderNightSkyBox(projection, -1 * dayPercentage + 1);
            skyRenderer.renderPlanets(projection);
        });
    }

    private void renderWorld() {
        worldRenderer.render();
    }

    private void renderUserInterface() {
        userInterface.draw();
    }

    private void renderCrosshair() {
        SpriteRenderer.PROGRAM.use(() -> {
            crosshairRenderer.update(new Vec2f(), 2, new Vec3f().extend(0.6f), true, true);
            crosshairRenderer.render();
        });
    }

    public int getFrame() {
        return frame;
    }

    public GeometryBuffer getGeometryBuffer() {
        return gBuffer;
    }

    public Texture2D getNoiseTexture() {
        return noiseTexture;
    }

    public SkyRenderer getSkyRenderer() {
        return skyRenderer;
    }

    private static Image generateNoiseImage(Vec2i resolution) {
        Noise simplex = new SimplexNoise(ThreadLocalRandom.current().nextInt());
        Noise fbm = new FBM(new SimplexNoise(ThreadLocalRandom.current().nextInt()), 3);

        BufferedImage img = new BufferedImage(resolution.x(), resolution.y(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < resolution.x(); x++) {
            for (int y = 0; y < resolution.y(); y++) {
                int r = (int) (ThreadLocalRandom.current().nextFloat() * 256f);
                int g = (int) ((simplex.sample(new Vec2f(x, y)) * 0.5 + 0.5) * 256f);
                int b = (int) ((fbm.sample(new Vec2f(x, y)) * 0.5 + 0.5) * 256f);
                final Color color = new Color(r, g, b);

                img.setRGB(x, y, color.getRGB());
            }
        }
        img.flush();

        return new Image(img);
    }

    private static CubemapTexture loadSkybox() throws IOException {
        final Image[] images = new Image[6];
        for (int i = 0; i < 6; i++) {
            images[i] = new Image(Util.readResource("textures/skybox/" + i + ".jpg"));
        }

        return SkyboxRenderer.createCubemap(images);
    }

}
