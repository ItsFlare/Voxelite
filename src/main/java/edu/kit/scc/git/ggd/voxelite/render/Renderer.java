package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.ui.UserInterface;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Matrix3f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.textures.CubemapTexture;
import net.durchholz.beacon.util.Image;
import net.durchholz.beacon.window.Viewport;
import net.durchholz.beacon.window.Window;
import org.lwjgl.opengl.GL30;

import java.io.IOException;

import static java.lang.Math.sin;
import static org.lwjgl.opengl.GL30.*;

public class Renderer {

    private final Camera         camera;
    private final UserInterface  userInterface;
    private final WorldRenderer  worldRenderer;
    private final SkyRenderer skyRenderer = new SkyRenderer();
    private final SpriteRenderer crosshairRenderer = new SpriteRenderer(new Image(Util.readResource("textures/crosshair.png")));
    private final GeometryBuffer             gBuffer           = new GeometryBuffer(1, 1);

    private Viewport viewport;

    public boolean renderUI     = true;
    public boolean renderSkybox = true;
    public boolean renderWorld  = true;
    public boolean wireframe    = false;

    private int frame;

    public Renderer(Window window) throws IOException {
        this.camera = new Camera(window);
        this.userInterface = new UserInterface();
        this.worldRenderer = new WorldRenderer();
        this.viewport = window.getViewport();
        gBuffer.allocate(viewport.width(), viewport.height());
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
        System.out.println("Rendering frame " + getFrame());

        updateViewport();
        OpenGL.polygonMode(OpenGL.Face.BOTH, wireframe ? OpenGL.PolygonMode.LINE : OpenGL.PolygonMode.FILL);

        gBuffer.use(() -> {
            OpenGL.setDrawBuffers(GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3);
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

    private void updateViewport() {
        final Viewport v = Main.INSTANCE.getWindow().getViewport();
        if (!viewport.equals(v)) {
            OpenGL.setViewport(v);
            viewport = v;
            gBuffer.allocate(viewport.width(), viewport.height());
        }
    }

    private void renderSky() {
        float dayPercentage = Util.clamp((float) sin(2 * Math.PI * Main.getDayPercentage()) + 0.75f, 0, 1);
        Vec2f viewportRes = new Vec2f(viewport.width(),viewport.height());

        var projection = camera.projection();
        projection.multiply(camera.view(false, true));

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

    private static CubemapTexture loadSkybox() throws IOException {
        final Image[] images = new Image[6];
        for (int i = 0; i < 6; i++) {
            images[i] = new Image(Util.readResource("textures/skybox/" + i + ".jpg"));
        }

        return SkyboxRenderer.createCubemap(images);
    }
}
