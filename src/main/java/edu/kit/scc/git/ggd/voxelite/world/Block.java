package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.render.QuadTexture;
import edu.kit.scc.git.ggd.voxelite.render.RenderType;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.Vec2f;
import org.jetbrains.annotations.NotNull;

public enum Block {

    DIRT;

    private final QuadTexture[] quads = new QuadTexture[Direction.values().length];

    @NotNull
    public RenderType getRenderType() {
        return RenderType.OPAQUE; //TODO Implement
    }

    @NotNull
    public QuadTexture getTexture(Direction direction) {
        return new QuadTexture(new Vec2f(0, 0),  new Vec2f(0, 1), new Vec2f(1, 1), new Vec2f(1, 0)); //TODO Implement
    }

}
