package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.render.RenderType;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public enum Block {

    AIR(Builder::transparent) {
        @Override
        public @NotNull Vec2i getTexture(Direction direction) {
            return thr();
        }

        @Override
        public @NotNull RenderType getRenderType() {
            return thr();
        }

        private static <T> T thr() {
            throw new UnsupportedOperationException("Air has no properties");
        }
    },
    BEDROCK(Builder::texture),
    DIRT(Builder::texture),
    STONE(Builder::texture),
    SAND(Builder::texture),
    COBBLESTONE(Builder::texture),
    OAK_LOG(builder -> builder.texture("log_oak").texture("log_oak_top", Direction.POS_Y, Direction.NEG_Y)),
    OAK_LEAVES(builder -> builder.texture("leaves_big_oak").cutout()),

    ACACIA_LOG(builder -> builder.texture("log_acacia").texture("log_acacia_top", Direction.POS_Y, Direction.NEG_Y)),

    BIRCH_LOG(builder -> builder.texture("log_birch").texture("log_birch_top", Direction.POS_Y, Direction.NEG_Y)),
    GRASS(builder -> builder.texture("grass_side").texture("dirt", Direction.NEG_Y).texture("grass_top", Direction.POS_Y)),
    WATER(builder -> builder.texture("water").transparent()),
    TNT(builder -> builder.texture("tnt_side").texture("tnt_bottom", Direction.NEG_Y).texture("tnt_top", Direction.POS_Y)),
    CACTUS(builder -> builder.texture("cactus_side").texture("cactus_top", Direction.POS_Y).texture("cactus_bottom", Direction.NEG_Y)),
    RED_GLASS(builder -> builder.texture("glass_red").transparent().filter(new Vec3f(1, 0, 0))),
    CYAN_GLASS(builder -> builder.texture("glass_cyan").transparent().filter(new Vec3f(0, 1, 1))),
    WHITE_GLASS(builder -> builder.texture("glass_white").transparent()),

    ICE(Builder::texture),

    COPPER_ORE(Builder::texture),

    GLOWSTONE(builder -> builder.texture().light(new Vec3f(1, 0, 0), 31));

    public Vec3f light, filter;
    public int compressedLight, compressedFilter;

    private final Vec2i[] quads;
    private final int lightRange;
    private final boolean opaque, lightSource, cutout;

    Block() {
        this(builder -> {});
    }

    Block(Consumer<Builder> builder) {
        final Builder b = new Builder(name().toLowerCase());
        builder.accept(b);

        this.quads = b.quads;

        this.light = b.light;
        this.lightRange = b.lightRange;
        this.lightSource = !b.light.equals(new Vec3f()) && lightRange > 0;
        this.filter = b.filter;
        this.compressedLight = CompressedLightStorage.encode(light, lightRange);
        this.compressedFilter = CompressedLightStorage.encode(filter, LightStorage.MAX_COMPONENT_VALUE);

        this.opaque = b.opaque && !b.cutout;
        this.cutout = b.cutout;
    }

    @NotNull
    public RenderType getRenderType() {
        return isOpaque() || isCutout() ? RenderType.OPAQUE : RenderType.TRANSPARENT;
    }

    @NotNull
    public Vec2i getTexture(Direction direction) {
        return quads[direction.ordinal()];
    }

    @NotNull
    public Vec3f getLight() {
        return light;
    }

    public int getLightRange() {
        return lightRange;
    }

    public int getCompressedLight() {
        return compressedLight;
    }

    public int getCompressedFilter() {
        return compressedFilter;
    }

    @Nullable
    public Vec3f getFilter() {
        return filter;
    }

    public boolean isLightSource() {
        return lightSource;
    }

    public boolean isOpaque() {
        return opaque;
    }

    public boolean isTransparent() {
        return !opaque;
    }

    public boolean isCutout() {
        return cutout;
    }

    private static class Builder {
        private final String name;
        private final Vec2i[] quads = new Vec2i[Direction.values().length]; //TODO Default values
        private Vec3f light = new Vec3f();
        private Vec3f filter = new Vec3f(1);
        private boolean opaque = true, cutout = false;
        private int lightRange = 0;

        private Builder(String name) {
            this.name = name;
        }

        public Builder texture() {
            return texture(name);
        }

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
            Vec2i uv = Main.INSTANCE.getRenderer().getWorldRenderer().getAtlas().getSprite(name);
            if(uv == null) throw new IllegalArgumentException("Texture %s not found".formatted(name));

            for (Direction direction : directions) {
                quads[direction.ordinal()] = uv; //TODO Rotate
            }

            return this;
        }

        public Builder transparent() {
            this.opaque = false;
            return this;
        }

        public Builder cutout() {
            this.cutout = true;
            return this;
        }

        public Builder light(Vec3f light, int range) {
            if(light.min() < 0) throw new IllegalArgumentException("Light values must not be negative");

            this.light = light;
            this.lightRange = range;

            return this;
        }

        public Builder filter(Vec3f filter) {
            this.filter = filter;
            return this;
        }

    }

}
