#version 410

in ivec2 tex;
in ivec3 pos;
in ivec3 normal;
in uint data;
in uint light;

out vec2 Tex;
out vec3 Pos;
flat out ivec3 Normal;
out vec3 BlockLight;
out vec3 LightSpacePos;

uniform mat4 mvp;
uniform ivec3 chunk;
uniform float normalizedSpriteSize;
uniform int maxLightValue;
uniform mat4 lightView;

void main() {
    uint x = data >> 27;
    uint y = (data >> 22) & uint(0x1f);
    uint z = (data >> 17) & uint(0x1f);

    uint u = (data >> 10) & uint(0x7f);
    uint v = (data >> 3) & uint(0x7f);

    ivec3 vp = chunk + pos + ivec3(x, y, z);
    gl_Position = mvp * vec4(vp, 1);

    Tex = vec2(ivec2(u, v) + tex) * normalizedSpriteSize;
    Pos = vec3(vp);
    Normal = normal;
    BlockLight = vec3(light >> 20, (light >> 10) & uint(0x3ff), light & uint(0x3ff)) / maxLightValue;
    LightSpacePos = (lightView * vec4(vp, 1)).xyz;
}