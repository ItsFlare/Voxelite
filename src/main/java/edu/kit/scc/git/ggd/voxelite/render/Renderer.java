package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.ui.UserInterface;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.textures.CubemapTexture;
import net.durchholz.beacon.util.Image;
import net.durchholz.beacon.window.Viewport;
import net.durchholz.beacon.window.Window;

import java.io.IOException;

public class Renderer {

    private final Camera         camera;
    private final UserInterface  userInterface;
    private final WorldRenderer  worldRenderer;
    private final SkyboxRenderer skyboxRenderer = new SkyboxRenderer(loadSkybox());

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

        if (renderSkybox) renderSkybox();
        if (renderWorld) renderWorld();

        if (wireframe) OpenGL.polygonMode(OpenGL.Face.BOTH, OpenGL.PolygonMode.FILL);
        if (renderUI) renderUserInterface();
    }

    public void tick() {
        worldRenderer.tick();
    }

    private void updateViewport() {
        final Viewport v = Main.INSTANCE.getWindow().getViewport();
        if (!viewport.equals(v)) {
            OpenGL.setViewport(v);
            viewport = v;
        }
    }

    private void renderUserInterface() {
        userInterface.tick();
    }

    private void renderSkybox() {
        final Matrix4f projection = camera.projection();
        projection.multiply(camera.view(false, true));
        skyboxRenderer.render(projection);
    }

    private void renderWorld() {
        worldRenderer.render();
    }

    private static CubemapTexture loadSkybox() throws IOException {
        final Image[] images = new Image[6];
        for (int i = 0; i < 6; i++) {
            images[i] = new Image(Util.readResource("textures/skybox/" + i + ".jpg"));
        }

        return SkyboxRenderer.createCubemap(images);
    }
}
