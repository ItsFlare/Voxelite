package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.ShaderLoader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class PostProgram extends ScreenProgram {
    public final Sampler composite   = sampler("composite");

    public final Uniform<Integer> aliasingOn = uniInteger("aliasingOn");

    public PostProgram() {
        super(ShaderLoader.get("screen.vs"), ShaderLoader.get("fxaa.fs"));
    }
}
