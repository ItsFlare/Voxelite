package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.texture.TextureAtlas;
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
    private final Main         main;
    private final TextureAtlas atlas;
    public Vec3f               lightColor = new Vec3f(1);
    public float               ambientStrength = 0.2f, diffuseStrength = 1.0f, specularStrength = 0.5f;

    public WorldRenderer(Main main) {
        this.main = main;
        EventType.addListener(this);

        try {
            atlas = new TextureAtlas("/textures/blocks");
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Listener
    private void onChunkLoad(ChunkLoadEvent event) {
        final RenderChunk renderChunk = new RenderChunk(event.getChunk());
        renderChunks.put(event.getChunk().getPosition(), renderChunk);
    }

    @Listener
    private void onChunkUnload(ChunkUnloadEvent event) {
        renderChunks.remove(event.getChunk().getPosition()).delete();
    }

    public void updateMeshes() {
        renderChunks.values().forEach(renderChunk -> renderChunk.updateMesh(main.getRenderer().getCamera().getPosition()));
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

                program.mvp.set(main.getRenderer().getCamera().transform());
                program.atlas.bind(0, atlas);
                program.camera.set(main.getRenderer().getCamera().getPosition());
                program.lightColor.set(lightColor);
                program.lightDirection.set(new Vec3f(0, -1, 0));
                program.ambientStrength.set(ambientStrength);
                program.diffuseStrength.set(diffuseStrength);
                program.specularStrength.set(specularStrength);

                for (RenderChunk renderChunk : renderChunks.values()) {
                    renderChunk.render(renderType, main.getRenderer().getCamera().getPosition());
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
