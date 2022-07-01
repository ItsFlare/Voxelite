package edu.kit.scc.git.ggd.voxelite.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompressedBlockStorageTest {

    private ChunkStorage<Block> storage, control;

    @BeforeEach
    void setUp() {
        storage = new CompressedBlockStorage();
        control = new NaiveBlockStorage();
    }

    @Test
    void test() {
        final int max = Chunk.VOLUME;
        for (int i = 0; i < max; i++) {
            final Block block = Block.values()[ThreadLocalRandom.current().nextInt(Block.values().length)];
            storage.set(i, block);
            control.set(i, block);
            assertEquals(block, storage.get(i));
        }

        for (int i = 0; i < max; i++) {
            assertEquals(control.get(i), storage.get(i), Integer.toString(i));
        }
    }
}