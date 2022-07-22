package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.render.opengl.OpenGL;

public class PostRenderer extends ScreenRenderer {

    private static final PostProgram PROGRAM = new PostProgram();

    private final BloomBlurRenderer bloomBlurRenderer = new BloomBlurRenderer();

    public int   bloomBlurIterations;
    public float exposure, gamma, bloomIntensity;
    public boolean bloom, aa, hdr, gammaCorrect;

    public PostRenderer() {
        super(PROGRAM);
    }

    public void render(GeometryBuffer gBuffer) {
        if (bloom) bloomBlurRenderer.render(gBuffer, bloomBlurIterations);

        OpenGL.use(OpenGL.STATE, PROGRAM, va, () -> {
            PROGRAM.composite.bind(0, gBuffer.composite());
            PROGRAM.bloom.bind(1, gBuffer.bloom());

            PROGRAM.aaEnabled.set(aa ? 1 : 0);

            PROGRAM.hdrEnabled.set(hdr ? 1 : 0);
            PROGRAM.exposure.set(exposure);

            PROGRAM.gammaEnabled.set(gammaCorrect ? 1 : 0);
            PROGRAM.gamma.set(gamma);

            PROGRAM.bloomEnabled.set(bloom ? 1 : 0);
            PROGRAM.bloomIntensity.set(bloomIntensity);

            drawScreen();
        });
    }
}