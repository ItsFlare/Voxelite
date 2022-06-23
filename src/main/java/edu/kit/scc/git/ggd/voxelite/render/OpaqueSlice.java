package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.util.Tuple;
import net.durchholz.beacon.data.IntVector;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.VBO;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;

//TODO Subclass per RenderType?
public class OpaqueSlice extends Slice {

    private static final OpaqueChunkProgram PROGRAM = (OpaqueChunkProgram) RenderType.OPAQUE.getProgram();

    protected final VBO commandBuffer = new VBO();

    protected Command[]               commands = new Command[1 << Direction.values().length];
    protected Tuple<int[], Command[]> nextCommands;

    public OpaqueSlice(RenderType renderType) {
        super(renderType);

        OpenGL.use(vertexArray, () -> {
            ChunkProgram.QUAD_VB.use(() -> {
                vertexArray.set(PROGRAM.position, ChunkProgram.QuadVertex.POSITION, ChunkProgram.QUAD_VB, 0);
                vertexArray.set(PROGRAM.texture, ChunkProgram.QuadVertex.TEXTURE, ChunkProgram.QUAD_VB, 0);
                vertexArray.set(PROGRAM.normal, ChunkProgram.QuadVertex.NORMAL, ChunkProgram.QUAD_VB, 0);
                vertexArray.set(PROGRAM.tangent, ChunkProgram.QuadVertex.TANGENT, ChunkProgram.QUAD_VB, 0);
                vertexArray.set(PROGRAM.bitangent, ChunkProgram.QuadVertex.BITANGENT, ChunkProgram.QUAD_VB, 0);
            });

            instanceBuffer.use(() -> {
                vertexArray.set(PROGRAM.data, ChunkProgram.InstanceVertex.DATA, instanceBuffer, 1);
            });

            aoBuffer.use(() -> {
                vertexArray.set(PROGRAM.ao, ChunkProgram.AOVertex.AO, aoBuffer, 4);
            });

            lightBuffer.use(() -> {
                vertexArray.set(PROGRAM.light, ChunkProgram.InstanceLightVertex.LIGHT, lightBuffer, 1);
            });
        });

        var shadowProgram = ShadowMapRenderer.PROGRAM;
        OpenGL.use(shadowVertexArray, () -> {
            ChunkProgram.QUAD_VB.use(() -> {
                shadowVertexArray.set(shadowProgram.position, ChunkProgram.QuadVertex.POSITION, ChunkProgram.QUAD_VB, 0);
            });

            instanceBuffer.use(() -> {
                shadowVertexArray.set(shadowProgram.data, ChunkProgram.InstanceVertex.DATA, instanceBuffer, 1);
            });
        });
    }

    @Override
    public synchronized void build() {
        if (queue == null || queue.isEmpty()) {
            quadCount = 0;
            return;
        }

        var queuedQuads = queue.stream().sorted(Comparator.comparingInt(value -> value.direction().ordinal())).toList();

        this.nextVertices = toInstanceVertices(queuedQuads);
        this.nextAOVertices = toAOVertices(queuedQuads);
        this.nextLightVertices = toLightVertices(queuedQuads);
        this.nextCommands = generateCommands();

        queue.clear();
    }

    @Override
    public synchronized void upload() {
        super.upload();
        if (this.nextCommands == null) return;

        commandBuffer.use(() -> commandBuffer.data(OpenGL.Usage.STREAM_DRAW, nextCommands.a()));
        commands = nextCommands.b();

        this.nextCommands = null;
    }

    @Override
    public void delete() {
        super.delete();
        commandBuffer.delete();
    }

    private Tuple<int[], Command[]> generateCommands() {
        Command[] cmds = new Command[1 << Direction.values().length];

        //Calculate partition sizes
        int[] directionCounts = new int[Direction.values().length];
        for (QueuedQuad queuedQuad : queue) {
            directionCounts[queuedQuad.direction().ordinal()]++;
        }

        //For all bitset permutations, precompute command data arrays
        final List<OpenGL.DrawMultiArraysIndirectCommand> commandList = new ArrayList<>(Direction.values().length);
        for (int i = 0; i < cmds.length; i++) {
            int commandOffset = commandList.size();

            int quadOffset = 0;
            for (int dir = 0; dir < Direction.values().length; dir++) {
                int directionQuadCount = directionCounts[dir];
                if (directionQuadCount == 0) continue;

                //IF visible
                if ((i & (1 << dir)) != 0) {
                    commandList.add(new OpenGL.DrawMultiArraysIndirectCommand(4, directionQuadCount, 4 * dir, quadOffset));
                }

                quadOffset += directionQuadCount;
            }

            cmds[i] = new Command(commandList.size() - commandOffset, commandOffset);
        }

        return new Tuple<>(IntVector.merge(commandList.toArray(OpenGL.DrawMultiArraysIndirectCommand[]::new)), cmds);
    }

    @Override
    public void render(VertexArray va, int visibility) {
        if (quadCount == 0) return;

        if (visibility == 0) return;
        final var cmd = commands[visibility];
        if (cmd.commands() == 0) return;

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, commandBuffer.id());
        va.use(() -> OpenGL.drawMultiArraysIndirect(OpenGL.Mode.TRIANGLE_STRIP, cmd.commands(), cmd.offset()));
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
    }
}
