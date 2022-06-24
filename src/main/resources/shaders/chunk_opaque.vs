#version 410

in ivec2 tex;
in ivec3 pos;
in ivec3 normal;
in uint data;
in uint light;
in uint ao;
in ivec3 tangent;
in ivec3 bitangent;

out vec2 Tex;
out vec3 Pos;
flat out ivec3 Normal;
out vec4 BlockLight;
out vec3 ViewSpacePos;
out vec3 LightSpacePos;
flat out mat3 TBN;
out float aoFactor;

uniform mat4 mvp;
uniform mat4 view;
uniform ivec3 chunk;
uniform float normalizedSpriteSize;
uniform int maxLightValue;
uniform mat4 lightView;

const float aoMap[4] = float[4](0.5, 0.7, 0.9, 1);

void main() {
    vec3 T = normalize(vec3(tangent));
    vec3 B = normalize(vec3(bitangent));
    vec3 N = normalize(vec3(normal));

    uint x = data >> 27;
    uint y = (data >> 22) & uint(0x1f);
    uint z = (data >> 17) & uint(0x1f);

    uint u = (data >> 10) & uint(0x7f);
    uint v = (data >> 3) & uint(0x7f);

    ivec3 vp = chunk + pos + ivec3(x, y, z);
    gl_Position = mvp * vec4(vp, 1);

    TBN = mat3(T, B, N);
    Tex = vec2(ivec2(u, v) + tex) * normalizedSpriteSize;
    Pos = vec3(vp);
    Normal = normal;
    BlockLight = vec4(light >> 20, (light >> 10) & uint(0x3ff), light & uint(0x3ff), 0) / maxLightValue;
    ViewSpacePos = (view * vec4(vp, 1)).xyz;
    LightSpacePos = (lightView * vec4(vp, 1)).xyz;
    aoFactor = aoMap[(ao >> ((gl_VertexID & 3) << 1)) & 3u];
}