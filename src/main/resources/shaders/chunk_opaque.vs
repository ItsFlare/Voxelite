#version 410

in ivec2 tex;
in vec3 pos;
in vec3 normal;
in uint data;
in uint light;

out vec2 Tex;
out vec3 Pos;
out vec3 Normal;
out vec4 BlockLight;
out vec4 LightSpacePos;

uniform mat4 mvp;
uniform ivec3 chunk;
uniform float normalizedSpriteSize;
uniform int maxLightValue;
uniform mat4 lightView;

void main() {
    uint x = data >> 27;
    uint y = (data >> 22) & uint(0x1f);
    uint z = (data >> 17) & uint(0x1f);

    uint u = (data >> 8) & uint(0xff);
    uint v = data & uint(0xff);

    vec3 vp = vec3(chunk) + pos + vec3(x, y, z);
    gl_Position = mvp * vec4(vp, 1);

    Tex = vec2(ivec2(u, v) + tex) * normalizedSpriteSize;
    Pos = vp;
    Normal = normal;
    BlockLight = vec4(light >> 20, (light >> 10) & uint(0x3ff), light & uint(0x3ff), 0) / maxLightValue;
    LightSpacePos = lightView * vec4(vp, 1);
}