package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class ChunkShadowProgram extends Program {

    public ChunkShadowProgram(Shader... shaders) {
        super(shaders);
    }

    public final Attribute<Vec3f>   position = attribute("pos", OpenGL.Type.FLOAT, 3);
    public final Attribute<Integer> data     = attribute("data", OpenGL.Type.INT, 1);

    public final Uniform<Matrix4f> mvp            = uniMatrix4f("mvp", true);
    public final Uniform<Vec3i>    chunk          = uniVec3i("chunk");
}
