package edu.kit.scc.git.ggd.voxelite.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompressedBlockStorageTest {

    private BlockStorage storage, control;

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
            storage.setBlock(i, block);
            control.setBlock(i, block);
            assertEquals(block, storage.getBlock(i));
        }

        for (int i = 0; i < max; i++) {
            assertEquals(control.getBlock(i), storage.getBlock(i), Integer.toString(i));
        }
    }
}