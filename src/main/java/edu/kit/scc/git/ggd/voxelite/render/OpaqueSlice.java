package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
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

    protected final Command[]                                   commands = new Command[1 << Direction.values().length];
    protected       OpenGL.DrawMultiArraysIndirectCommand[][] nextCommands;

    public OpaqueSlice(RenderType renderType) {
        super(renderType);

        for (int i = 0; i < commands.length; i++) {
            commands[i] = new Command(new VBO(), 0);
        }

        OpenGL.use(vertexArray, () -> {
            ChunkProgram.QUAD_VB.use(() -> {
                vertexArray.set(PROGRAM.position, ChunkProgram.QuadVertex.POSITION, ChunkProgram.QUAD_VB, 0);
                vertexArray.set(PROGRAM.texture, ChunkProgram.QuadVertex.TEXTURE, ChunkProgram.QUAD_VB, 0);
                vertexArray.set(PROGRAM.normal, ChunkProgram.QuadVertex.NORMAL, ChunkProgram.QUAD_VB, 0);
            });

            instanceBuffer.use(() -> {
                vertexArray.set(PROGRAM.data, ChunkProgram.InstanceVertex.DATA, instanceBuffer, 1);
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
        this.nextLightVertices = toLightVertices(queuedQuads);
        this.nextCommands = generateCommands();

        queue.clear();
    }

    @Override
    public synchronized void upload() {
        super.upload();
        if (this.nextCommands == null) return;

        for (int i = 0; i < nextCommands.length; i++) {
            final var directionCommands = nextCommands[i];
            final var vbo = this.commands[i].buffer();

            vbo.use(() -> vbo.data(OpenGL.Usage.DYNAMIC_DRAW, IntVector.merge(directionCommands))); //TODO Merge in build
            this.commands[i] = new Command(vbo, directionCommands.length);
        }

        this.nextCommands = null;
    }

    @Override
    public void delete() {
        super.delete();
        for (Command command : commands) {
            command.buffer().delete();
        }
    }

    private OpenGL.DrawMultiArraysIndirectCommand[][] generateCommands() {
        OpenGL.DrawMultiArraysIndirectCommand[][] cmds = new OpenGL.DrawMultiArraysIndirectCommand[commands.length][];

        //Calculate partition sizes
        int[] directionCounts = new int[Direction.values().length];
        for (QueuedQuad queuedQuad : queue) {
            directionCounts[queuedQuad.direction().ordinal()]++;
        }

        //For all bitset permutations, precompute command data arrays
        for (int i = 0; i < cmds.length; i++) {
            final List<OpenGL.DrawMultiArraysIndirectCommand> dataList = new ArrayList<>(Direction.values().length);

            int offset = 0;
            for (int dir = 0; dir < Direction.values().length; dir++) {
                int directionQuadCount = directionCounts[dir];
                if (directionQuadCount == 0) continue;

                //IF visible
                if ((i & (1 << dir)) != 0) {
                    dataList.add(new OpenGL.DrawMultiArraysIndirectCommand(4, directionQuadCount, 4 * dir, offset));
                }

                offset += directionQuadCount;
            }

            final OpenGL.DrawMultiArraysIndirectCommand[] directionCommands = dataList.toArray(OpenGL.DrawMultiArraysIndirectCommand[]::new);
            cmds[i] = directionCommands;
        }

        return cmds;
    }

    @Override
    public void render(VertexArray va, int visibility) {
        if (quadCount == 0) return;

        if (visibility == 0) return;
        final var cmd = commands[visibility];
        if (cmd.commands() == 0) return;

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, cmd.buffer().id());
        va.use(() -> OpenGL.drawMultiArraysIndirect(OpenGL.Mode.TRIANGLE_STRIP, cmd.commands(), 0));
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
    }
}
