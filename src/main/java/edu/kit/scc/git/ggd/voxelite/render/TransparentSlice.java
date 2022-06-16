package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.IBO;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.stream.IntStream;

import static edu.kit.scc.git.ggd.voxelite.util.Util.debug;

public class TransparentSlice extends Slice {

    private static final TransparentChunkProgram PROGRAM = (TransparentChunkProgram) RenderType.TRANSPARENT.getProgram();
    private static final Logger                  LOGGER  = LoggerFactory.getLogger(TransparentSlice.class);

    private final Vec3i worldPosition;
    private final IBO   ibo = new IBO();

    private UploadedQuad[] quads;
    private long           lastSortTick;

    public TransparentSlice(Vec3i worldPosition, RenderType renderType) {
        super(renderType);
        this.worldPosition = worldPosition;

        OpenGL.use(vertexArray, ibo, () -> {
            instanceBuffer.use(() -> {
                vertexArray.set(PROGRAM.data, ChunkProgram.InstanceVertex.DATA, instanceBuffer, 0);
            });

            lightBuffer.use(() -> {
                vertexArray.set(PROGRAM.light, ChunkProgram.InstanceLightVertex.LIGHT, lightBuffer, 0);
            });
        });

        var shadowProgram = ShadowMapRenderer.PROGRAM;
        OpenGL.use(shadowVertexArray, ibo, () -> {
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

        var relative = Main.INSTANCE.getRenderer().getCamera().getPosition().subtract(worldPosition);

        var sorted = queue.stream()
                .sorted(Comparator.comparingDouble(value -> inverseDistance(value.position(), value.direction(), relative)))
                .toList();

        quads = sorted.stream().map(queuedQuad -> new UploadedQuad(queuedQuad.direction(), queuedQuad.position())).toArray(UploadedQuad[]::new);

        this.nextVertices = toInstanceVertices(sorted);
        this.nextLightVertices = toLightVertices(sorted);

        queue.clear();
    }

    @Override
    public synchronized void upload() {
        super.upload();

        ibo.use(() -> ibo.data(OpenGL.Usage.STREAM_DRAW, IntStream.range(0, quadCount).toArray()));
    }

    @Override
    public void render(VertexArray va, int visibility) {
        if (quadCount == 0) return;

        PROGRAM.visibility.set(visibility);
        va.use(() -> OpenGL.drawIndexed(OpenGL.Mode.POINTS, quadCount, OpenGL.Type.UNSIGNED_INT));
    }

    @Override
    public void delete() {
        super.delete();
        ibo.delete();
    }

    public synchronized void sort() {
        debug(() -> LOGGER.trace("Sorting (d=%.0f t=%d q=%d)".formatted(
                Chunk.toBlockPosition(Main.INSTANCE.getRenderer().getCamera().getPosition()).subtract(worldPosition.add(8)).magnitude(),
                Main.INSTANCE.getTick() - lastSortTick,
                quadCount)));

        if (quads == null) return;
        if (quadCount != quads.length) return; //TODO Work around errors caused by race condition build -> sort -> upload

        var relative = Main.INSTANCE.getRenderer().getCamera().getPosition().subtract(worldPosition);

        var indices = IntStream.range(0, quadCount).boxed().sorted(Comparator.comparingDouble(value -> {
            var quad = quads[value];
            return inverseDistance(quad.position(), quad.direction(), relative);
        })).mapToInt(value -> value).toArray();

        ibo.use(() -> ibo.data(OpenGL.Usage.STREAM_DRAW, indices));

        //Be careful when writing to lastSortTick as it may break Comparators
        lastSortTick = Main.INSTANCE.getTick();
    }

    public long getLastSortTick() {
        return lastSortTick;
    }

    private static double inverseDistance(Vec3i position, Direction direction, Vec3f relative) {
        return -position.add(0.5f).add(direction.getAxis().scale(0.5f)).subtract(relative).magnitudeSq();
    }
}
