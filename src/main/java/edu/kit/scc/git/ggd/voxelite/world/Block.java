package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.render.RenderType;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.Vec2i;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public enum Block {

    BEDROCK(builder -> builder.texture("bedrock")),
    DIRT(builder -> builder.texture("dirt")),
    STONE(builder -> builder.texture("stone")),
    SAND(builder -> builder.texture("sand")),
    COBBLESTONE(builder -> builder.texture("cobblestone")),
    OAK_LOG(builder -> builder.texture("oak_log").texture("oak_log_top", Direction.POS_Y, Direction.NEG_Y)),
    GRASS_PATH(builder -> builder.texture("grass_path_side").texture("dirt", Direction.NEG_Y).texture("grass_path_top", Direction.POS_Y)),
    TNT(builder -> builder.texture("tnt_side").texture("tnt_bottom", Direction.NEG_Y).texture("tnt_top", Direction.POS_Y));

    private final Vec2i[] quads;

    Block(Consumer<Builder> builder) {
        final Builder b = new Builder();
        builder.accept(b);

        this.quads = b.quads;
    }

    @NotNull
    public RenderType getRenderType() {
        return RenderType.OPAQUE; //TODO Implement
    }

    @NotNull
    public Vec2i getTexture(Direction direction) {
        return quads[direction.ordinal()];
    }

    private static class Builder {
        private final Vec2i[] quads = new Vec2i[Direction.values().length]; //TODO Default values

        public Builder texture(String name) {
            return texture(name, 0);
        }

        public Builder texture(String name, int rotation) {
            return texture(name, rotation, Direction.values());
        }

        public Builder texture(String name, Direction... directions) {
            return texture(name, 0, directions);
        }

        public Builder texture(String name, int rotation, Direction... directions) {
            Vec2i uv = Main.INSTANCE.getRenderer().getWorldRenderer().getAtlas().getSprite(name + ".png"); //TODO Atlas lookup
            for (Direction direction : directions) {
                quads[direction.ordinal()] = uv; //TODO Rotate
            }

            return this;
        }

    }

}
