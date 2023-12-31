package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;

import java.util.Arrays;

import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT3;

public class CompositeRenderer extends ScreenRenderer {

    public static final CompositeProgram PROGRAM = new CompositeProgram();

    public CompositeRenderer() {
        super(PROGRAM);
    }

    public void render(GeometryBuffer gBuffer) {
        OpenGL.use(OpenGL.STATE, PROGRAM, va, () -> {
            OpenGL.setDrawBuffers(GL_COLOR_ATTACHMENT3);
            OpenGL.resetState();
            OpenGL.depthTest(false);

            final Camera camera = Main.INSTANCE.getRenderer().getCamera();
            final WorldRenderer worldRenderer = Main.INSTANCE.getRenderer().getWorldRenderer();
            PROGRAM.debugRoughness.set(worldRenderer.debugRoughness);
            PROGRAM.opaque.bind(0, gBuffer.opaque());
            PROGRAM.normal.bind(1, gBuffer.normal());
            PROGRAM.mer.bind(2, gBuffer.mer());
            PROGRAM.depth.bind(3, gBuffer.depth());
            PROGRAM.projection.set(camera.projection());
            PROGRAM.reflections.set(worldRenderer.reflections ? 1 : 0);
            PROGRAM.coneTracing.set(worldRenderer.coneTracing ? 1 : 0);



            var shadowMapRenderer = Main.INSTANCE.getRenderer().getWorldRenderer().getShadowMapRenderer();
            PROGRAM.shadowMap.bind(4, shadowMapRenderer.getTexture());
            PROGRAM.constantBias.set(shadowMapRenderer.constantBias);
            PROGRAM.cascadeScales.set(Arrays.stream(shadowMapRenderer.c).map(ShadowMapRenderer.Cascade::scale).toArray(Vec3f[]::new));
            PROGRAM.cascadeTranslations.set(Arrays.stream(shadowMapRenderer.c).map(ShadowMapRenderer.Cascade::translation).toArray(Vec3f[]::new));
            PROGRAM.cascadeFar.set(Arrays.stream(shadowMapRenderer.c).map(ShadowMapRenderer.Cascade::far).toArray(Float[]::new));

            final Matrix4f inverseView = camera.view(true, true);
            inverseView.invert();
            final Matrix4f viewToLight = shadowMapRenderer.lightView(Main.INSTANCE.getWorld().getSunlightDirection());
            viewToLight.multiply(inverseView);

            PROGRAM.viewToLight.set(viewToLight);

            drawScreen();
        });
    }
}
