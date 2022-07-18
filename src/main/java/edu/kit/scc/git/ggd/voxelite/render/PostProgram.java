package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.ShaderLoader;

public class PostProgram extends ScreenProgram {
    public final Sampler composite   = sampler("composite");

    public PostProgram() {
        super(ShaderLoader.get("screen.vs"), ShaderLoader.get("fxaa.fs"));
    }
}
