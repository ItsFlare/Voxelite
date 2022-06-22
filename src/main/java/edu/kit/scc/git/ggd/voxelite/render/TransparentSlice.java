package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class TransparentSlice extends Slice {

    private static final TransparentChunkProgram PROGRAM = (TransparentChunkProgram) RenderType.TRANSPARENT.getProgram();
    private final Vec3i worldPosition;
    private QueuedQuad[] quads;

    public TransparentSlice(Vec3i worldPosition, RenderType renderType) {
        super(renderType);
        this.worldPosition = worldPosition;

        OpenGL.use(vertexArray, () -> {
            instanceBuffer.use(() -> {
                vertexArray.set(PROGRAM.data, ChunkProgram.InstanceVertex.DATA, instanceBuffer, 0);
            });

            aoBuffer.use(() -> {
                vertexArray.set(PROGRAM.ao, ChunkProgram.AOVertex.AO, aoBuffer, 4);
            });

            lightBuffer.use(() -> {
                vertexArray.set(PROGRAM.light, ChunkProgram.InstanceLightVertex.LIGHT, lightBuffer, 0);
            });
        });

        var shadowProgram = ShadowMapRenderer.PROGRAM;
        OpenGL.use(shadowVertexArray, () -> {
            instanceBuffer.use(() -> {
                shadowVertexArray.set(shadowProgram.data, ChunkProgram.InstanceVertex.DATA, instanceBuffer, 0);
            });
        });
    }

    @Override
    public synchronized void build() {
        if (queue == null || queue.isEmpty()) {
            quadCount = 0;
            return;
        }

        quads = queue.toArray(QueuedQuad[]::new);

        var queuedQuads = sortQuads();

        this.nextVertices = toInstanceVertices(queuedQuads);
        this.nextAOVertex = toAOVertices(queuedQuads);
        this.nextLightVertices = toLightVertices(queuedQuads);

        queue.clear();
    }

    @Override
    public void render(VertexArray va, int visibility) {
        if(quadCount == 0) return;

        PROGRAM.visibility.set(visibility);
        va.use(() -> {
            OpenGL.drawArrays(OpenGL.Mode.POINTS, 0, quadCount);
        });
    }

    public synchronized void sort() { //TODO Optimize
        if (quads == null) return;

        var queuedQuads = sortQuads();

        var v = toInstanceVertices(queuedQuads);
        var ao = toAOVertices(queuedQuads);
        var l = toLightVertices(queuedQuads);

        instanceBuffer.use(() -> {
            instanceBuffer.data(v);
        });

        aoBuffer.use(() -> {
            aoBuffer.data(ao);
        });

        lightBuffer.use(() -> {
            lightBuffer.data(l);
        });
    }

    private List<QueuedQuad> sortQuads() {
        var relative = Chunk.toBlockPosition(Main.INSTANCE.getRenderer().getCamera().getPosition()).subtract(worldPosition);

        return Arrays
                .stream(quads)
                .sorted(Comparator.comparingInt(value -> -value.position().subtract(relative).magnitudeSq()))
                .toList();
    }
}
