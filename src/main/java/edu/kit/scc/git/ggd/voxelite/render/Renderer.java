package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.ui.UserInterface;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Matrix3f;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.textures.CubemapTexture;
import net.durchholz.beacon.util.Image;
import net.durchholz.beacon.window.Viewport;
import net.durchholz.beacon.window.Window;

import java.io.IOException;

import static java.lang.Math.sin;

public class Renderer {

    private final Camera         camera;
    private final UserInterface  userInterface;
    private final WorldRenderer  worldRenderer;
    private final SkyRenderer skyRenderer = new SkyRenderer();
    private final SpriteRenderer crosshairRenderer = new SpriteRenderer(new Image(Util.readResource("textures/crosshair.png")));

    private Viewport viewport;

    public boolean renderUI     = true;
    public boolean renderSkybox = true;
    public boolean renderWorld  = true;
    public boolean wireframe    = false;

    public Renderer(Window window) throws IOException {
        this.camera = new Camera(window);
        this.userInterface = new UserInterface();
        this.worldRenderer = new WorldRenderer();
        this.viewport = window.getViewport();
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
        updateViewport();
        OpenGL.polygonMode(OpenGL.Face.BOTH, wireframe ? OpenGL.PolygonMode.LINE : OpenGL.PolygonMode.FILL);

        if (renderSkybox) renderSky();
        if (renderWorld) renderWorld();

        if (wireframe) OpenGL.polygonMode(OpenGL.Face.BOTH, OpenGL.PolygonMode.FILL);
        if (renderUI) {
            renderCrosshair();
            renderUserInterface();
        }
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
        }
    }

    private void renderSkybox() {
        final Matrix4f projection = camera.projection();
        projection.multiply(camera.view(false, true));
        //skyboxRenderer.render(projection);
    }

    private void renderSky() {
        Vec3f blueSky = new Vec3f(0.3f,0.55f,0.8f);
        Vec3f nightSky = new Vec3f();
        float dayPercentage = Util.clamp((float) sin(2 * Math.PI * Main.getDayPercentage()) + 0.75f, 0, 1);
        Vec2f viewportRes = new Vec2f(viewport.width(),viewport.height());

        Matrix3f rotation = Util.quatToMatrix(camera.getRotation());
        //System.out.println(camera.getDirection());
        //System.out.println(camera.getPosition());
        System.out.println(dayPercentage);
        //System.out.println(camera.view(false, true));

        skyRenderer.render(nightSky.interpolate(blueSky, dayPercentage),  camera.getDirection(), viewportRes, dayPercentage, camera.getFOV(), rotation);
        skyRenderer.renderNightSkyBox(camera.view(false, true), camera.projection(), -1 *dayPercentage + 1);
        skyRenderer.renderSun(camera.view(false, true), camera.projection());
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

    private static CubemapTexture loadSkybox() throws IOException {
        final Image[] images = new Image[6];
        for (int i = 0; i < 6; i++) {
            images[i] = new Image(Util.readResource("textures/skybox/" + i + ".jpg"));
        }

        return SkyboxRenderer.createCubemap(images);
    }
}
