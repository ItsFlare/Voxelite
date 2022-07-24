package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;

import static java.lang.Math.sin;

public class GodrayRenderer extends ScreenRenderer {

    public static final Vec3f SUN_NOON = new Vec3f(1, 0.9f, 0.5f);
    public static final Vec3f SUN_EVENING = new Vec3f(1, 0.3f, 0f);
    public static final Vec3f MOON = new Vec3f(0.25f, 0.25f, 0.3f);

    private static final GodrayProgram PROGRAM = new GodrayProgram();

    public int godraySamples;
    public float godrayDensity;
    public float godrayExposure;
    public float godrayNoiseFactor;

    public Vec3f godrayColorOverride = new Vec3f(0);

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
            PROGRAM.godrayColorOverride.set(godrayColorOverride);

            PROGRAM.lightView.set(Main.INSTANCE.getRenderer().getSkyRenderer().getLightView());
            PROGRAM.lightScreen.set(Main.INSTANCE.getRenderer().getSkyRenderer().getLightScreen());

            PROGRAM.fov.set(Main.INSTANCE.getRenderer().getCamera().getFOV());

            final float dayPercentage = Main.getDayPercentage();
            PROGRAM.sunColor.set(calculateSunColor(dayPercentage));
            PROGRAM.moonColor.set(calculateMoonColor(dayPercentage));

            drawScreen();
        });
    }

    public static Vec3f calculateSunColor(float dayPercentage) {
        dayPercentage = (float) sin(2 * Math.PI * dayPercentage);

        return SUN_EVENING.interpolate(SUN_NOON, Util.clamp(dayPercentage, 0, 1));
    }

    public static Vec3f calculateMoonColor(float dayPercentage) {
        return MOON;
    }
}
