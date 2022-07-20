package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.textures.Texture2D;

public class PostRenderer extends ScreenRenderer {

    private static final PostProgram PROGRAM = new PostProgram();

    public PostRenderer() {
        super(PROGRAM);
    }

    public void render(Texture2D outputTexture, int aliasing) {
        OpenGL.use(OpenGL.STATE, PROGRAM, va, () -> {
            PROGRAM.composite.bind(0, outputTexture);
            PROGRAM.antiAliasingOn.set(aliasing);

            drawScreen();
        });
    }
}
