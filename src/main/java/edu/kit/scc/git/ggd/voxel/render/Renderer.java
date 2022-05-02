package edu.kit.scc.git.ggd.voxel.render;

import edu.kit.scc.git.ggd.voxel.Main;
import edu.kit.scc.git.ggd.voxel.ui.UserInterface;
import edu.kit.scc.git.ggd.voxel.util.Util;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.textures.CubemapTexture;
import net.durchholz.beacon.util.Image;
import net.durchholz.beacon.window.Viewport;

import java.io.IOException;

public class Renderer {

    private final Main           main;
    private final Camera         camera;
    private final UserInterface  userInterface;
    private final SkyboxRenderer skyboxRenderer = new SkyboxRenderer(loadSkybox());

    private Viewport viewport;

    public boolean renderUI     = true;
    public boolean renderSkybox = true;
    public boolean wireframe    = false;

    public Renderer(Main main) throws IOException {
        this.main = main;
        this.camera = new Camera(main.getWindow());
        this.userInterface = new UserInterface(main);
        this.viewport = main.getWindow().getViewport();
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

    public void render() {
        updateViewport();
        OpenGL.polygonMode(OpenGL.Face.BOTH, wireframe ? OpenGL.PolygonMode.LINE : OpenGL.PolygonMode.FILL);

        if (renderSkybox) renderSkybox();

        OpenGL.polygonMode(OpenGL.Face.BOTH, OpenGL.PolygonMode.FILL);
        if (renderUI) renderUserInterface();
    }

    private void updateViewport() {
        final Viewport v = main.getWindow().getViewport();
        if(!viewport.equals(v)) {
            OpenGL.setViewport(v);
            viewport = v;
        }
    }

    private void renderUserInterface() {
        userInterface.tick();
    }

    public void renderSkybox() {
        final Matrix4f projection = camera.projection();
        projection.multiply(camera.view(false, true));
        skyboxRenderer.render(projection);
    }

    private static CubemapTexture loadSkybox() throws IOException {
        final Image[] images = new Image[6];
        for (int i = 0; i < 6; i++) {
            images[i] = new Image(Util.readResource("textures/skybox/" + i + ".jpg"));
        }

        return SkyboxRenderer.createCubemap(images);
    }
}
