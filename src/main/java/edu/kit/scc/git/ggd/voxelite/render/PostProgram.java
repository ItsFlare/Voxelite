package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.ShaderLoader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class PostProgram extends ScreenProgram {
    public final Sampler composite = sampler("composite");
    public final Sampler bloom     = sampler("bloom");
    public final Sampler godrays   = sampler("godrays");

    public final Uniform<Integer> aaEnabled      = uniInteger("aaEnabled");
    public final Uniform<Integer> bloomEnabled   = uniInteger("bloomEnabled");
    public final Uniform<Integer> hdrEnabled     = uniInteger("hdrEnabled");
    public final Uniform<Integer> gammaEnabled   = uniInteger("gammaEnabled");
    public final Uniform<Integer> godraysEnabled   = uniInteger("godraysEnabled");
    public final Uniform<Float>   bloomIntensity = uniFloat("bloomIntensity");
    public final Uniform<Float>   exposure       = uniFloat("exposure");
    public final Uniform<Float>   gamma          = uniFloat("gamma");

    public final Uniform<Integer> godrayBlurSamples = uniInteger("godrayBlurSamples");
    public final Uniform<Integer> godrayBlurLod     = uniInteger("godrayBlurLod");
    public final Uniform<Float>   godrayBlurStride  = uniFloat("godrayBlurStride");

    public PostProgram() {
        super(ShaderLoader.get("screen.vs"), ShaderLoader.get("post.fs"));
    }
}
