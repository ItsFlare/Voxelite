package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.texture.TextureAtlas;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkLoadEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkUnloadEvent;
import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.event.Listener;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WorldRenderer {

    private final Map<Vec3i, RenderChunk> renderChunks = new HashMap<>();
    private final TextureAtlas atlas;
    public Vec3f               lightColor = new Vec3f(1);
    public float               ambientStrength = 0.2f, diffuseStrength = 0.5f, specularStrength = 0.5f;

    public WorldRenderer() {
        EventType.addListener(this);

        try {
            atlas = new TextureAtlas("/textures/blocks");
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Listener
    private void onChunkLoad(ChunkLoadEvent event) {
        final RenderChunk renderChunk = new RenderChunk(event.chunk());
        final Vec3i position = event.chunk().getPosition();
        renderChunks.put(position, renderChunk);
        Main.INSTANCE.getExecutor().execute(() -> {
            renderChunk.build();
            rebuildNeighbors(position);
        });
    }

    @Listener
    private void onChunkUnload(ChunkUnloadEvent event) {
        final Vec3i position = event.chunk().getPosition();
        renderChunks.remove(position).delete();
        Main.INSTANCE.getExecutor().execute(() -> {
            rebuildNeighbors(position);
        });
    }

    public void rebuildNeighbors(Vec3i position) {
        for (Direction direction : Direction.values()) {
            var neighbor = renderChunks.get(position.add(direction.getAxis()));
            if(neighbor != null) neighbor.build();
        }
    }

    public void rebuildMeshes() {
        renderChunks.values().forEach(RenderChunk::build);
    }

    public void render() {
        OpenGL.depthTest(true);
        OpenGL.depthMask(true);

        final RenderType[] renderTypes = RenderType.values();
        for (int i = 0; i < renderTypes.length; i++) {
            if (i > 0) return; //TODO Remove with transparency

            final RenderType renderType = renderTypes[i];
            final ChunkProgram program = renderType.getProgram();

            program.use(() -> {
                OpenGL.primitiveRestart(true);
                OpenGL.primitiveRestartIndex(ChunkProgram.PRIMITIVE_RESET_INDEX);

                program.mvp.set(Main.INSTANCE.getRenderer().getCamera().transform());
                program.atlas.bind(0, atlas);
                program.camera.set(Main.INSTANCE.getRenderer().getCamera().getPosition());
                program.lightColor.set(lightColor);
                program.lightDirection.set(new Vec3f(0, -1, 0));
                program.ambientStrength.set(ambientStrength);
                program.diffuseStrength.set(diffuseStrength);
                program.specularStrength.set(specularStrength);
                program.normalizedSpriteSize.set(atlas.getNormalizedSpriteSize());

                for (RenderChunk renderChunk : renderChunks.values()) {
                    renderChunk.render(renderType);
                }
            });
        }
    }

    public Collection<RenderChunk> getRenderChunks() {
        return renderChunks.values();
    }

    public RenderChunk getRenderChunk(Vec3i position) {
        return renderChunks.get(position);
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }
}
