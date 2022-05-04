package edu.kit.scc.git.ggd.voxelite.world.event;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.event.Event;
import net.durchholz.beacon.event.EventType;

public class ChunkUnloadEvent implements Event {
    public static final EventType<ChunkUnloadEvent> TYPE = new EventType<>(ChunkUnloadEvent.class);

    private final Chunk chunk;

    public ChunkUnloadEvent(Chunk chunk) {
        this.chunk = chunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    @Override
    public EventType<ChunkUnloadEvent> getType() {
        return TYPE;
    }
}
