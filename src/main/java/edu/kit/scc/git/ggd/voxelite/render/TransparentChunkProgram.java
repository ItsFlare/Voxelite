package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

import java.util.Arrays;

public class TransparentChunkProgram extends ChunkProgram {

    public final Uniform<Vec3i[]>  normals        = uniVec3iArray("normals", 6);
    public final Uniform<Vec3i[]>  vertices       = uniVec3iArray("vertices", 6 * 4);
    public final Uniform<Vec2i[]>  texCoords      = uniVec2iArray("texCoords", 6 * 4);
    public final Uniform<Integer>  visibility     = uniInteger("visibility");
    public final Uniform<Float>    debugRoughness = uniFloat("debugRoughness");
    public final Uniform<Integer>  reflections    = uniInteger("reflections");
    public final Uniform<Integer>  coneTracing    = uniInteger("coneTracing");
    public final Uniform<Matrix4f> projection     = uniMatrix4f("projection", true);

    public final Sampler opaque = sampler("opaque");
    public final Sampler depth  = sampler("depth");

    public TransparentChunkProgram(Shader... shaders) {
        super(shaders);

        use(() -> {
            normals.set(Arrays.stream(Direction.values()).map(Direction::getAxis).toArray(Vec3i[]::new));
            vertices.set(Arrays.stream(QUAD_VERTICES).map(QuadVertex::position).toArray(Vec3i[]::new));
            texCoords.set(Arrays.stream(QUAD_VERTICES).map(QuadVertex::texture).toArray(Vec2i[]::new));
            visibility.set(RenderChunk.FULL_VISIBILITY);
        });
    }

}
