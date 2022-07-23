package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.ShaderLoader;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class CompositeProgram extends ScreenProgram {

    public final Uniform<Float> debugRoughness      = uniFloat("debugRoughness");
    public final Sampler        opaque      = sampler("opaque");
    public final Sampler        normal      = sampler("normal");
    public final Sampler        mer         = sampler("mer");
    public final Sampler        depth       = sampler("depth");

    public final Uniform<Matrix4f> projection = uniMatrix4f("projection", true);
    public final Uniform<Matrix4f> viewToLight    = uniMatrix4f("viewToLight", true);

    public final Uniform<Integer> reflections = uniInteger("reflections");
    public final Uniform<Integer> coneTracing = uniInteger("coneTracing");

    public final Sampler          shadowMap           = sampler("shadowMap");
    public final Uniform<Float>   constantBias        = uniFloat("constantBias");
    public final Uniform<Vec3f[]> cascadeScales       = uniVec3fArray("cascades", "scale", 4);
    public final Uniform<Vec3f[]> cascadeTranslations = uniVec3fArray("cascades", "translation", 4);
    public final Uniform<Float[]> cascadeFar          = uniFloatArray("cascades", "far", 4);
    public final Uniform<Integer> kernel              = uniInteger("kernel");


    public CompositeProgram() {
        super(ShaderLoader.get("screen.vs"), ShaderLoader.get("composite.fs"));
    }

}
