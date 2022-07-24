package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;

public class GodrayRenderer extends ScreenRenderer {

    private static final GodrayProgram PROGRAM = new GodrayProgram();

    public int godraySamples;
    public float godrayDensity;
    public float godrayExposure;
    public float godrayNoiseFactor;

    public Vec3f godrayColor = new Vec3f(1);

    public GodrayRenderer() {
        super(PROGRAM);
    }

    public void render(GeometryBuffer gBuffer) {
        OpenGL.use(OpenGL.STATE, PROGRAM, va, () -> {
            PROGRAM.composite.bind(0, gBuffer.composite());
            PROGRAM.depth.bind(1, gBuffer.depth());
            PROGRAM.noise.bind(2, Main.INSTANCE.getRenderer().getNoiseTexture());

            PROGRAM.godraySamples.set(godraySamples);
            PROGRAM.godrayExposure.set(godrayExposure);
            PROGRAM.godrayDensity.set(godrayDensity);
            PROGRAM.godrayNoiseFactor.set(godrayNoiseFactor);

            PROGRAM.lightView.set(Main.INSTANCE.getRenderer().getSkyRenderer().getLightView());
            PROGRAM.lightScreen.set(Main.INSTANCE.getRenderer().getSkyRenderer().getLightScreen());

            PROGRAM.fov.set(Main.INSTANCE.getRenderer().getCamera().getFOV());

            drawScreen();
        });
    }
}
