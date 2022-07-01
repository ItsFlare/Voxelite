package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.render.RenderChunk;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import net.durchholz.beacon.math.AABB;
import net.durchholz.beacon.math.Vec3i;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import static edu.kit.scc.git.ggd.voxelite.world.Chunk.toLinearSpace;
import static edu.kit.scc.git.ggd.voxelite.world.Chunk.toWorldPosition;

public class WorldChunk implements Chunk {

    private final World                  world;
    private final Vec3i                  position;
    private final AABB                   boundingBox;
    private final ChunkStorage<Block>    blockStorage = new CompressedBlockStorage();
    private final CompressedLightStorage lightStorage = new CompressedLightStorage();
    private final VisibilityStorage      visibilityStorage = new VisibilityStorage() {
        @Override
        public Set<Direction> floodFill(int linear) {
            throw new IllegalStateException("Use copy for flood-fill"); //TODO Make neater, also copy class != class
        }
    };

    private int blockCount = 0;


    public WorldChunk(World world, Vec3i position) {
        this.world = world;
        this.position = position;
        this.boundingBox = BOUNDING_BOX.translate(toWorldPosition(position));
    }

    public WorldChunk(GeneratorChunk<?> generatorChunk) {
        this.world = generatorChunk.getGenerator().getWorld();
        this.position = generatorChunk.getPosition();
        this.boundingBox = BOUNDING_BOX.translate(toWorldPosition(position));

        final var lightSources = new ArrayList<Voxel>();
        for (Voxel voxel : generatorChunk) {
            final int linear = toLinearSpace(voxel.position());
            final Block block = voxel.getBlock();
            if(block == Block.AIR) continue;
            blockCount++;

            blockStorage.set(linear, block);
            visibilityStorage.set(linear, block.isOpaque());
            if(block.isLightSource()) lightSources.add(voxel);
        }

        ForkJoinPool.commonPool().submit(() -> {
            for (Voxel lightSource : lightSources) {
                lightStorage.calculate(lightSource, Block.AIR);
            }
        });
    }

    @Override
    public Block getBlock(Vec3i position) {
        return blockStorage.get(toLinearSpace(position));
    }

    @Override
    public void setBlock(final Vec3i position, final Block block) {
        final Voxel voxel = getVoxel(position);
        final int linear = toLinearSpace(position);
        final Block previous = blockStorage.get(linear);

        blockStorage.set(linear, block);
        visibilityStorage.set(linear, block.isOpaque());

        ForkJoinPool.commonPool().submit(() -> {
            lightStorage.calculate(voxel, previous); //TODO Batch light updates?
        });

        //TODO Optimize?
        if (previous == Block.AIR && block != Block.AIR) blockCount += 1;
        else if (previous != Block.AIR && block == Block.AIR) blockCount -= 1;

        //Store chunks to update
        final Set<Vec3i> chunks = new HashSet<>();

        //Add current chunk
        chunks.add(this.position);

        //Add neighboring chunks
        for (Direction direction : Direction.values()) {
            final Voxel neighbor = voxel.getNeighbor(direction);
            if (neighbor != null) chunks.add(neighbor.chunk().getPosition());
        }

        //TODO Causes slight flicker because of rebuild during light update
        //Update all affected chunks
        for (Vec3i chunkPosition : chunks) {
            final RenderChunk renderChunk = Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunk(chunkPosition);
            if (renderChunk != null) Main.INSTANCE.getRenderer().getWorldRenderer().queueRebuild(renderChunk); //TODO Make neater
        }
    }

    public CompressedLightStorage getLightStorage() {
        return lightStorage;
    }

    public VisibilityStorage getVisibilityStorage() {
        return visibilityStorage;
    }

    @Override
    public boolean isOpaque(Vec3i position) {
        return visibilityStorage.get(position);
    }

    public World getWorld() {
        return world;
    }

    @Override
    public ChunkDomain getDomain() {
        return world;
    }

    @Override
    public Vec3i getPosition() {
        return position;
    }

    @Override
    public AABB getBoundingBox() {
        return boundingBox;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public static Iterable<Vec3i> iterate() {
        return Util.cuboid(new Vec3i(), EXTENT);
    }

}
