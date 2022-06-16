package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

import java.util.Arrays;

import static org.lwjgl.opengl.GL43.*;

public class TransparentChunkProgram extends ChunkProgram {

    public final Uniform<Vec3i[]> normals    = uniVec3iArray("normals", 6);
    public final Uniform<Vec3i[]> vertices   = uniVec3iArray("vertices", 6 * 4);
    public final Uniform<Vec2i[]> texCoords  = uniVec2iArray("texCoords", 6 * 4);
    public final Uniform<Integer> visibility = uniInteger("visibility");

    public TransparentChunkProgram(Shader... shaders) {
        super(shaders);

        use(() -> {
            normals.set(Arrays.stream(Direction.values()).map(Direction::getAxis).toArray(Vec3i[]::new));
            vertices.set(Arrays.stream(QUAD_VERTICES).map(QuadVertex::position).toArray(Vec3i[]::new));
            texCoords.set(Arrays.stream(QUAD_VERTICES).map(QuadVertex::texture).toArray(Vec2i[]::new));
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
