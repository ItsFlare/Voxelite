package edu.kit.scc.git.ggd.voxelite.render.event;

import net.durchholz.beacon.event.Event;
import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.math.Vec3f;

public record CameraMoveEvent(Vec3f previous, Vec3f current) implements Event {
    public static final EventType<CameraMoveEvent> TYPE = new EventType<>(CameraMoveEvent.class);

    @Override
    public EventType<?> getType() {
        return TYPE;
    }
}