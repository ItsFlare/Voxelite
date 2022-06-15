package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

import static org.lwjgl.opengl.GL43.*;

public class TransparentChunkProgram extends ChunkProgram {

    public final Uniform<Vec3i[]> normals    = uniVec3iArray("normals", 6);
    public final Uniform<Vec3i[]> vertices   = uniVec3iArray("vertices", 6 * 4);
    public final Uniform<Vec2i[]> texCoords  = uniVec2iArray("texCoords", 6 * 4);
    public final Uniform<Integer> visibility = uniInteger("visibility");

    public TransparentChunkProgram(Shader... shaders) {
        super(shaders);

        Vec3i[] n = new Vec3i[6];
        Vec3i[] v = new Vec3i[6 * 4];
        Vec2i[] t = new Vec2i[6 * 4];

        Direction[] values = Direction.values();
        for (int i = 0; i < values.length; i++) {
            Direction direction = values[i];
            n[i] = direction.getAxis();

            v[i * 4 + 0] = direction.getUnitQuad().v0();
            v[i * 4 + 1] = direction.getUnitQuad().v3();
            v[i * 4 + 2] = direction.getUnitQuad().v1();
            v[i * 4 + 3] = direction.getUnitQuad().v2();

            t[i * 4 + 0] = new Vec2i(0, 0);
            t[i * 4 + 1] = new Vec2i(1, 0);
            t[i * 4 + 2] = new Vec2i(0, 1);
            t[i * 4 + 3] = new Vec2i(1, 1);
        }

        use(() -> {
            normals.set(n);
            vertices.set(v);
            texCoords.set(t);
            visibility.set(RenderChunk.FULL_VISIBILITY);
        });
    }

    public static void printLayout(int id) {
        int numActiveAttribs = glGetProgramInterfacei(id, GL_PROGRAM_INPUT, GL_ACTIVE_RESOURCES);

        for(int attrib = 0; attrib < numActiveAttribs; ++attrib) {
            String name = glGetProgramResourceName(id, GL_PROGRAM_INPUT, attrib);
            System.out.println(name);
        }

        int numActiveUniforms = glGetProgramInterfacei(id, GL_UNIFORM, GL_ACTIVE_RESOURCES);
        for (int i = 0; i < numActiveUniforms; i++) {
            String name = glGetProgramResourceName(id, GL_UNIFORM, i);
            System.out.println(name);
        }
    }
}
