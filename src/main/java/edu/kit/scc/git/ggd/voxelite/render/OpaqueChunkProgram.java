package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.shader.Shader;

public class OpaqueChunkProgram extends ChunkProgram {
    public OpaqueChunkProgram(Shader... shaders) {
        super(shaders);
    }

    public final Attribute<Vec3f> position = attribute("pos", OpenGL.Type.FLOAT, 3);
    public final Attribute<Vec2i> texture  = attribute("tex", OpenGL.Type.INT, 2);
    public final Attribute<Vec3f> normal   = attribute("normal", OpenGL.Type.FLOAT, 3);
}
