package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;

import java.util.Arrays;

public class CompositeRenderer {

    public static final CompositeProgram PROGRAM = new CompositeProgram();

    public static final CompositeProgram.QuadVertex[] QUAD_VERTICES = new CompositeProgram.QuadVertex[]{
            new CompositeProgram.QuadVertex(new Vec2f(-1, -1)),
            new CompositeProgram.QuadVertex(new Vec2f(1, -1)),
            new CompositeProgram.QuadVertex(new Vec2f(-1, 1)),
            new CompositeProgram.QuadVertex(new Vec2f(1, 1))
    };

    public static final VertexBuffer<CompositeProgram.QuadVertex> VB = new VertexBuffer<>(CompositeProgram.QuadVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.STATIC_DRAW);

    static {
        VB.use(() -> VB.data(QUAD_VERTICES));
    }

    private final VertexArray va = new VertexArray();

    public CompositeRenderer() {
        OpenGL.use(va, VB, () -> va.set(PROGRAM.pos, CompositeProgram.QuadVertex.POSITION, VB, 0));
    }

    public void render(GeometryBuffer gBuffer) {
        OpenGL.use(OpenGL.STATE, PROGRAM, va, () -> {
            OpenGL.resetState();
            OpenGL.depthTest(true);
            OpenGL.depthFunction(OpenGL.CompareFunction.ALWAYS); //Disable depth testing the other way

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

            OpenGL.drawArrays(OpenGL.Mode.TRIANGLE_STRIP, 0, 4);
        });
    }

}
