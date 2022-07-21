package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.ShaderLoader;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class BlurProgram extends ScreenProgram {
    public final Uniform<Vec2f> direction = uniVec2f("direction");
    public final Sampler sampler = sampler("sampler");

    public BlurProgram() {
        super(ShaderLoader.get("screen.vs"), ShaderLoader.get("blur.fs"));
    }

}
