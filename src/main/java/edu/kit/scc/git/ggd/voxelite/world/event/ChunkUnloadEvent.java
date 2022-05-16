package edu.kit.scc.git.ggd.voxelite.world.event;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.event.Event;
import net.durchholz.beacon.event.EventType;

public record ChunkUnloadEvent(Chunk chunk) implements Event {
    public static final EventType<ChunkUnloadEvent> TYPE = new EventType<>(ChunkUnloadEvent.class);

    @Override
    public EventType<ChunkUnloadEvent> getType() {
        return TYPE;
    }
}
