package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.ShaderLoader;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class GodrayProgram extends ScreenProgram {
    public final Sampler composite = sampler("composite");
    public final Sampler depth     = sampler("depth");
    public final Sampler noise     = sampler("noise");

    public final Uniform<Integer> godraySamples     = uniInteger("godraySamples");
    public final Uniform<Float>   godrayDensity     = uniFloat("godrayDensity");
    public final Uniform<Float>   godrayExposure    = uniFloat("godrayExposure");
    public final Uniform<Float>   godrayNoiseFactor = uniFloat("godrayNoiseFactor");
    public final Uniform<Vec3f>   lightView         = uniVec3f("lightView");
    public final Uniform<Vec2f>   lightScreen       = uniVec2f("lightScreen");
    public final Uniform<Float>   fov               = uniFloat("fov");

    public GodrayProgram() {
        super(ShaderLoader.get("screen.vs"), ShaderLoader.get("vl.fs"));
    }
}
