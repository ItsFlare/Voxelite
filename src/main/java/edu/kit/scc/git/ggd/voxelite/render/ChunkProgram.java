package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.*;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class ChunkProgram extends Program {

    public static final int MAX_UNSIGNED_SHORT    = (1 << (Short.BYTES * 8)) - 1;
    public static final int PRIMITIVE_RESET_INDEX = MAX_UNSIGNED_SHORT;

    private static final int     INDEX_COUNT       = 4;
    private static final int     INDEX_COUNT_PLUS  = INDEX_COUNT + 1;
    private static final short[] QUAD_INDICES      = {0, 3, 1, 2};
    private static final short[] UNORDERED_INDICES = Slice.generateIndexBuffer(IntStream.range(0, Chunk.VOLUME * 6).toArray());

    /**
     * This order of directions is biased under the assumption that usually more populated chunks are below than above.
     * Because POS_Y is in the center, the average batch size scales with the ratio of populated chunks below the camera.
     * Directions are also grouped by their sign because opposite directions are rarely active simultaneously.
     */
    private static final Direction[] DIRECTIONS         = {Direction.POS_X, Direction.POS_Z, Direction.POS_Y, Direction.NEG_X, Direction.NEG_Z, Direction.NEG_Y};
    private static final int[]       DIRECTION_ORDINALS = Arrays.stream(Direction.values()).mapToInt(value -> Arrays.asList(DIRECTIONS).indexOf(value)).toArray();

    public static boolean directionCulling = true;

    public ChunkProgram(Shader... shaders) {
        super(shaders);
    }

    public final Attribute<Integer> position = attribute("pos", OpenGL.Type.INT, 1);
    public final Attribute<Vec2f>   texture  = attribute("tex", OpenGL.Type.FLOAT, 2);
    public final Attribute<Vec3f>   normal   = attribute("normal", OpenGL.Type.FLOAT, 3);

    public final Uniform<Matrix4f> mvp              = uniMatrix4f("mvp", true);
    public final Uniform<Vec3i>    chunk            = uniVec3i("chunk");
    public final Sampler           atlas            = sampler("atlas");
    public final Uniform<Vec3f>    camera           = uniVec3f("camera");
    public final Uniform<Vec3f>    lightDirection   = uniVec3f("light.direction");
    public final Uniform<Vec3f>    lightColor       = uniVec3f("light.color");
    public final Uniform<Float>    ambientStrength  = uniFloat("ambientStrength");
    public final Uniform<Float>    diffuseStrength  = uniFloat("diffuseStrength");
    public final Uniform<Float>    specularStrength = uniFloat("specularStrength");

    public record MeshVertex(short position, Vec2f texture, Vec3f normal) implements Vertex {
        public static final VertexLayout<MeshVertex> LAYOUT   = new VertexLayout<>(MeshVertex.class);
        public static final VertexAttribute<Integer> POSITION = LAYOUT.primitive(false);
        public static final VertexAttribute<Vec2f>   TEXTURE  = LAYOUT.vec2f(false);
        public static final VertexAttribute<Vec3f>   NORMAL   = LAYOUT.vec3f(false);

        @Override
        public VertexLayout<?> getLayout() {
            return LAYOUT;
        }
    }

    //TODO Subclass per RenderType?
    public class Slice {

        record QueuedQuad(Direction direction, QuadMesh mesh, QuadTexture texture, short quadPosition) {}

        protected final Chunk      chunk;
        protected final RenderType renderType;

        protected final VertexArray              vertexArray = new VertexArray();
        protected final VertexBuffer<MeshVertex> meshBuffer  = new VertexBuffer<>(MeshVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.DYNAMIC_DRAW);
        protected final IBO                      ibo         = new IBO();
        protected final List<QueuedQuad>         queue       = new ArrayList<>();

        protected short[]         vertexPositions       = new short[0];
        protected short[]         quadPositions         = new short[0];
        protected CommandData[][] directionDrawCommands = new CommandData[1 << DIRECTIONS.length][];

        public Slice(Chunk chunk, RenderType renderType) {
            this.chunk = chunk;
            this.renderType = renderType;

            OpenGL.use(vertexArray, ibo, () -> {
                meshBuffer.use(() -> {
                    vertexArray.set(ChunkProgram.this.position, MeshVertex.POSITION, meshBuffer, 0);
                    vertexArray.set(ChunkProgram.this.texture, MeshVertex.TEXTURE, meshBuffer, 0);
                    vertexArray.set(ChunkProgram.this.normal, MeshVertex.NORMAL, meshBuffer, 0);
                });

//                lightBuffer.use(() -> {
//                    vertexArray.set(ChunkProgram.this.blockLight, LightVertex.BLOCK_LIGHT, lightBuffer, 0);
//                    vertexArray.set(ChunkProgram.this.skyLight, LightVertex.SKY_LIGHT, lightBuffer, 0);
//                });
            });
        }

        record CommandData(int offset, int size) {}

        public void upload(Vec3f cameraPosition) {
            if (queue.isEmpty()) return;
            int quadCount = queue.size();

            quadPositions = new short[quadCount];
            MeshVertex[] meshVertices = new MeshVertex[quadCount * 4];

            //Extract vertices from queue
            for (int i = 0; i < quadCount; i++) {
                QueuedQuad queuedQuad = queue.get(i);

                //Store quad positions for later use like sorting
                quadPositions[i] = queuedQuad.quadPosition();

                Vec3f normal = new Vec3f(queuedQuad.direction.getAxis());
                meshVertices[i * 4 + 0] = new MeshVertex((short) packVector(queuedQuad.mesh().v0()), queuedQuad.texture().uv0(), normal);
                meshVertices[i * 4 + 1] = new MeshVertex((short) packVector(queuedQuad.mesh().v1()), queuedQuad.texture().uv1(), normal);
                meshVertices[i * 4 + 2] = new MeshVertex((short) packVector(queuedQuad.mesh().v2()), queuedQuad.texture().uv2(), normal);
                meshVertices[i * 4 + 3] = new MeshVertex((short) packVector(queuedQuad.mesh().v3()), queuedQuad.texture().uv3(), normal);
            }

            //Upload mesh
            meshBuffer.use(() -> {
                meshBuffer.data(meshVertices);
            });

            //Store vertex positions for later use like light sampling
            vertexPositions = new short[meshVertices.length];
            for (int i = 0; i < meshVertices.length; i++) {
                assert meshVertices[i].position() < MAX_UNSIGNED_SHORT;
                vertexPositions[i] = (short) meshVertices[i].position();
            }

            //Calculate index buffer
            final short[] indices;
            if (renderType == RenderType.TRANSPARENT) {
                indices = sortQuads(quadPositions, cameraPosition);
            } else {
                //Technically we only need this if direction culling is enabled, but we don't want to rebuild all chunks when toggling
                indices = generateDirectionalIndexBuffer();
            }

            //Upload index buffer
            ibo.use(() -> {
                ibo.data(OpenGL.Usage.DYNAMIC_DRAW, indices);
            });

            queue.clear();
        }

        private short[] generateDirectionalIndexBuffer() {

            //Sort quads by direction and calculate partition sizes
            short[] indices = sortQuads(queue.stream().map(Slice.QueuedQuad::direction).toArray(Direction[]::new));
            int[] directionCounts = new int[Direction.values().length];
            for (Slice.QueuedQuad queuedQuad : queue) {
                directionCounts[DIRECTION_ORDINALS[queuedQuad.direction().ordinal()]]++;
            }

            //For all bitset permutations, precompute command data arrays
            for (int i = 0; i < directionDrawCommands.length; i++) {
                List<Slice.CommandData> dataList = new ArrayList<>(3);

                int offset = 0, accumulator = 0;
                for (int j = 0; j < DIRECTIONS.length; j++) {
                    int directionQuadCount = directionCounts[j];
                    if (directionQuadCount == 0) continue;

                    if ((i & (1 << j)) != 0) {
                        accumulator += directionQuadCount;
                    } else {
                        if (accumulator > 0) {
                            dataList.add(new Slice.CommandData(offset, accumulator));

                            offset += accumulator;
                            accumulator = 0;
                        }

                        offset += directionQuadCount;
                    }
                }

                if (accumulator > 0) {
                    dataList.add(new Slice.CommandData(offset, accumulator));
                }

                final Slice.CommandData[] commandData = dataList.toArray(Slice.CommandData[]::new);
                directionDrawCommands[i] = commandData;
            }

            return indices;
        }

        public void render(Vec3f cameraPosition) {
            if (quadPositions.length == 0) return;

            ChunkProgram.this.chunk.set(Chunk.toWorldPosition(chunk.getPosition()));

            if (!directionCulling
                    || Chunk.toChunkPosition(new Vec3i(cameraPosition)).equals(chunk.getPosition()) //TODO Fix check if camera inside chunk
                    || renderType == RenderType.TRANSPARENT) {

                //Render everything
                vertexArray.use(() -> {
                    OpenGL.drawIndexed(OpenGL.Mode.TRIANGLE_STRIP, quadPositions.length * INDEX_COUNT_PLUS, OpenGL.Type.UNSIGNED_SHORT);
                });
            } else {
                /*
                Direction culling (indices are partitioned by face direction)
                Also batches adjacent active partitions (for a non-empty chunk: best case 1 draw call, worst case 3)

                TODO Calculate average case - does direction order matter if quads are not uniformly distributed?
                TODO Unroll loop and replace dot product with comparison?
                TODO Assume active directions for a chunk don't change often -> calculate grouping per chunk (Would this cause lag for fast movement?)
                TODO Automatically enable when crossover point is reached (depending on quad count)
                TODO Fall back to drawing everything in worst case?
                */

                final Vec3i chunkCenter = Chunk.toWorldPosition(chunk.getPosition()).add(Chunk.CENTER);
                final Direction[] directions = DIRECTIONS;

                //Calculate visibility bitset
                int bitset = 0;
                for (int i = 0; i < directions.length; i++) {

                    final var direction = directions[i];
                    final var planePos = chunkCenter.subtract(direction.getAxis().scale(Chunk.WIDTH >> 1));
                    final var planeToCam = cameraPosition.subtract(planePos);

                    if (planeToCam.dot(direction.getAxis()) > 0f) {
                        bitset |= (1 << i);
                    }
                }

                int finalBitset = bitset;
                vertexArray.use(() -> {
                    //Render precomputed batches based on visibility
                    for (CommandData data : directionDrawCommands[finalBitset]) {
                        OpenGL.drawIndexed(OpenGL.Mode.TRIANGLE_STRIP, data.size * INDEX_COUNT_PLUS, OpenGL.Type.UNSIGNED_SHORT, data.offset * INDEX_COUNT_PLUS);
                    }
                });
            }
        }

        //TODO Check if rounded camera position is sufficient
        private short[] sortQuads(short[] quadPositions, Vec3f cameraPosition) {
            final int[] order = IntStream.range(0, quadPositions.length)
                    .boxed()
                    .sorted(Comparator.<Integer>comparingDouble(quadindex -> cameraPosition.subtract(unpackVector(quadPositions[quadindex])).magnitudeSq()).reversed())
                    .mapToInt(value -> value)
                    .toArray();

            return generateIndexBuffer(order);
        }

        private static short[] sortQuads(Direction[] quadDirections) {
            final int[] order = IntStream.range(0, quadDirections.length)
                    .boxed()
                    .sorted(Comparator.comparingInt(value -> DIRECTION_ORDINALS[quadDirections[value].ordinal()]))
                    .mapToInt(value -> value)
                    .toArray();

            return generateIndexBuffer(order);
        }

        private static short[] generateIndexBuffer(int[] order) {
            short[] result = new short[order.length * INDEX_COUNT_PLUS];

            for (int i = 0; i < order.length; i++) {
                for (int j = 0; j < INDEX_COUNT; j++) {
                    final int index = order[i] * INDEX_COUNT + QUAD_INDICES[j];
                    assert index < MAX_UNSIGNED_SHORT;

                    result[i * INDEX_COUNT_PLUS + j] = (short) index;
                }
                result[i * INDEX_COUNT_PLUS + INDEX_COUNT] = (short) PRIMITIVE_RESET_INDEX;
            }

            return result;
        }

        private static final int Y_SHIFT = Chunk.WIDTH_EXP + 1;
        private static final int X_SHIFT = Y_SHIFT + Chunk.HEIGHT_EXP + 1;

        private static final int Z_MASK = (1 << (Chunk.WIDTH_EXP + 1)) - 1;
        private static final int Y_MASK = ((1 << (Chunk.HEIGHT_EXP + 1)) - 1) << Y_SHIFT;
        private static final int X_MASK = Z_MASK << X_SHIFT;

        //Pack a chunk-space vector. x, z E [0, CHUNK_WIDTH], y E [0, CHUNK_HEIGHT]
        public static int packVector(Vec3i vec3i) {
            assert vec3i.x() >= 0 && vec3i.x() <= Chunk.WIDTH;
            assert vec3i.y() >= 0 && vec3i.y() <= Chunk.HEIGHT;
            assert vec3i.z() >= 0 && vec3i.z() <= Chunk.WIDTH;

            return (vec3i.x() << X_SHIFT) | (vec3i.y() << Y_SHIFT) | vec3i.z();
        }

        public static Vec3i unpackVector(int packed) {
            int x, y, z;
            x = (packed & X_MASK) >>> X_SHIFT;
            y = (packed & Y_MASK) >>> Y_SHIFT;
            z = (packed & Z_MASK);

            return new Vec3i(x, y, z);
        }
    }
}
