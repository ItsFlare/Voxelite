package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Vec2f;

public record QuadTexture(Vec2f uv0, Vec2f uv1, Vec2f uv2, Vec2f uv3, RenderType renderType) {}
