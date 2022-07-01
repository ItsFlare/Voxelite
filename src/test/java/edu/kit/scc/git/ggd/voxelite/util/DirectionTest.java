package edu.kit.scc.git.ggd.voxelite.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectionTest {

    @Test
    void getOpposite() {
        for (Direction direction : Direction.values()) {
            assertEquals(direction.getAxis().scale(-1), direction.getOpposite().getAxis());
        }
        
        assertEquals(Direction.POS_X, Direction.NEG_X.getOpposite());
        assertEquals(Direction.NEG_X, Direction.POS_X.getOpposite());

        assertEquals(Direction.POS_Y, Direction.NEG_Y.getOpposite());
        assertEquals(Direction.NEG_Y, Direction.POS_Y.getOpposite());

        assertEquals(Direction.POS_Z, Direction.NEG_Z.getOpposite());
        assertEquals(Direction.NEG_Z, Direction.POS_Z.getOpposite());
    }
}