package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.LightStorage;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;

import java.util.ArrayList;
import java.util.List;

public abstract class Slice {
    protected final RenderType                                renderType;
    protected final VertexArray                               vertexArray       = new VertexArray();
    protected final VertexArray                               shadowVertexArray = new VertexArray();
    protected final VertexBuffer<ChunkProgram.InstanceVertex> instanceBuffer    = new VertexBuffer<>(ChunkProgram.InstanceVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.DYNAMIC_DRAW);

    protected final VertexBuffer<ChunkProgram.AOVertex>            aoBuffer    = new VertexBuffer<>(ChunkProgram.AOVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.DYNAMIC_DRAW);
    protected final VertexBuffer<ChunkProgram.InstanceLightVertex> lightBuffer = new VertexBuffer<>(ChunkProgram.InstanceLightVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.DYNAMIC_DRAW);

    protected List<QueuedQuad>              queue = new ArrayList<>();
    protected ChunkProgram.InstanceVertex[] nextVertices;

    protected ChunkProgram.AOVertex[]            nextAOVertices;
    protected ChunkProgram.InstanceLightVertex[] nextLightVertices;
    protected int                                quadCount;

    public Slice(RenderType renderType) {
        this.renderType = renderType;
    }

    public abstract void build();

    public abstract void render(VertexArray va, int visibility);

    public void render(int visibility) {
        render(vertexArray, visibility);
    }

    public void renderShadow(int visibility) {
        render(shadowVertexArray, visibility);
    }

    public int getQuadCount() {
        return quadCount;
    }

    public synchronized void upload() {
        if (nextVertices == null) return;
        assert nextVertices.length == nextLightVertices.length;
        assert nextVertices.length == nextAOVertices.length;

        instanceBuffer.use(() -> {
            instanceBuffer.data(nextVertices);
        });

        aoBuffer.use(() -> {
            aoBuffer.data(nextAOVertices);
        });

        lightBuffer.use(() -> {
            lightBuffer.data(nextLightVertices);
        });

        this.quadCount = nextVertices.length;
        this.nextVertices = null;
        this.nextAOVertices = null;
        this.nextLightVertices = null;
    }

    public void delete() {
        vertexArray.delete();
        instanceBuffer.delete();
        aoBuffer.delete();
        lightBuffer.delete();
    }

    public void queue(QueuedQuad quad) {
        queue.add(quad);
    }

    protected static ChunkProgram.InstanceLightVertex[] toLightVertices(List<QueuedQuad> queuedQuads) {
        return queuedQuads.stream()
                .map(queuedQuad -> new ChunkProgram.InstanceLightVertex(packLight(queuedQuad.light())))
                .toArray(ChunkProgram.InstanceLightVertex[]::new);
    }

    protected static ChunkProgram.InstanceVertex[] toInstanceVertices(List<QueuedQuad> queuedQuads) {
        return queuedQuads.stream()
                .map(queuedQuad -> new ChunkProgram.InstanceVertex(packInstance(queuedQuad)))
                .toArray(ChunkProgram.InstanceVertex[]::new);
    }

    protected static ChunkProgram.AOVertex[] toAOVertices(List<QueuedQuad> queuedQuads) {
        return queuedQuads.stream()
                .map(queuedQuad -> new ChunkProgram.AOVertex(queuedQuad.ao))
                .toArray(ChunkProgram.AOVertex[]::new);
    }

    public static int packInstance(QueuedQuad queuedQuad) {
        return Slice.packInstance(queuedQuad.position(), queuedQuad.texture(), queuedQuad.direction().ordinal());
    }

    public static int packInstance(Vec3i offset, Vec2i texture, int direction) {
        assert offset.max() < 32;
        assert texture.x() < 128 && texture.y() < 128;
        assert direction < 6;

        int result = 0;

        result |= offset.x() << 27;
        result |= offset.y() << 22;
        result |= offset.z() << 17;

        result |= texture.x() << 10;
        result |= texture.y() << 3;

        result |= direction;

        return result;
    }

    public static Vec3i unpackPosition(int instance) {
        int x = instance >> 27;
        int y = (instance >> 22) & 0x1f;
        int z = (instance >> 17) & 0x1f;
        return new Vec3i(x, y, z);
    }

    public static int packLight(Vec3i light) {
        assert LightStorage.MAX_TOTAL_VALUE < (1 << 10);
        assert light.x() < 1024 && light.y() < 1024 && light.z() < 1024;

        int result = 0;

        result |= light.x() << 20;
        result |= light.y() << 10;
        result |= light.z();

        return result;
    }

    record QueuedQuad(Direction direction, Vec3i position, Vec2i texture, Vec3i light, byte ao) {}

    record UploadedQuad(Direction direction, Vec3i position) {}

    record Command(int commands, int offset) {}
}
