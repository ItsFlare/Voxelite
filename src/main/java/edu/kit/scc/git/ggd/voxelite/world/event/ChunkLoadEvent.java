package edu.kit.scc.git.ggd.voxelite.world.event;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.event.Event;
import net.durchholz.beacon.event.EventType;

public record ChunkLoadEvent(Chunk chunk) implements Event {
    public static final EventType<ChunkLoadEvent> TYPE = new EventType<>(ChunkLoadEvent.class);

    @Override
    public EventType<ChunkLoadEvent> getType() {
        return TYPE;
    }
}
