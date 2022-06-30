package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class OpaqueChunkProgram extends ChunkProgram {
    public OpaqueChunkProgram(Shader... shaders) {
        super(shaders);
    }

    public final Attribute<Vec3i>  position  = attribute("pos", OpenGL.Type.INT, 3);
    public final Attribute<Vec2i>  texture   = attribute("tex", OpenGL.Type.INT, 2);
    public final Attribute<Vec3i>  normal    = attribute("normal", OpenGL.Type.INT, 3);
    public final Attribute<Vec3i>  tangent   = attribute("tangent", OpenGL.Type.INT, 3);
    public final Attribute<Vec3i>  bitangent = attribute("bitangent", OpenGL.Type.INT, 3);
    public final Uniform<Matrix4f> view      = uniMatrix4f("view", true);
}
