package edu.kit.scc.git.ggd.voxelite.world.generator;

import edu.kit.scc.git.ggd.voxelite.world.*;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass.GeneratorPass;
import net.durchholz.beacon.math.Vec3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GeneratorChunk<G extends MultiPassGenerator<G>> implements Chunk {
    public static final Logger LOGGER = LoggerFactory.getLogger(GeneratorChunk.class);

    private final G generator;
    private final ChunkDomain domain;
    private final Vec3i               position;
    private final ChunkStorage<Block>                            blockStorage = new NaiveBlockStorage();
    private final Lock                                           lock         = new ReentrantLock();
    private       GeneratorPass<G>    pass;

    private final HeightMap heightMap = new HeightMap();

    public GeneratorChunk(G generator, ChunkDomain domain, Vec3i position) {
        this.pass = generator.getFirstPass();
        this.generator = generator;
        this.domain = domain;
        this.position = position;
    }

    @Override
    public Vec3i getPosition() {
        return position;
    }

    @Override
    public ChunkDomain getDomain() {
        return domain;
    }

    @Override
    public Block getBlock(Vec3i position) {
        return blockStorage.get(Chunk.toLinearSpace(position));
    }

    @Override
    public void setBlock(Vec3i position, Block block) {
        blockStorage.set(Chunk.toLinearSpace(position), block);
    }

    @Override
    public CompressedLightStorage getLightStorage() {
        throw new UnsupportedOperationException(); //TODO ?
    }

    public Lock getLock() {
        return lock;
    }

    public GeneratorPass<G> getPass() {
        return pass;
    }

    public void generate() {
        LOGGER.debug("Generating %s for %s".formatted(pass, position));
        pass.apply(generator, this);
        pass = pass.getChild();
    }

    public MultiPassGenerator<G> getGenerator() {
        return generator;
    }

    public HeightMap getHeightMap() {
        return heightMap;
    }
}
