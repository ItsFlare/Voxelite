package edu.kit.scc.git.ggd.voxelite.world.event;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.event.Event;
import net.durchholz.beacon.event.EventType;

public class ChunkLoadEvent implements Event {
    public static final EventType<ChunkLoadEvent> TYPE = new EventType<>(ChunkLoadEvent.class);

    private final Chunk chunk;

    public ChunkLoadEvent(Chunk chunk) {
        this.chunk = chunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    @Override
    public EventType<ChunkLoadEvent> getType() {
        return TYPE;
    }
}
