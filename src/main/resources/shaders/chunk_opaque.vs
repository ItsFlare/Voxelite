#version 410

#include "include\util.glsl"
#include "include\ao.glsl"

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
out vec3 BlockLight;
out vec3 ViewSpacePos;
out vec3 LightSpacePos;
flat out mat3 TBN;

uniform mat4 mvp;
uniform mat4 view;
uniform ivec3 chunk;
uniform float normalizedSpriteSize;
uniform int maxLightValue;
uniform mat4 lightView;

void main() {
    ivec3 vp = chunk + pos + decodePosition(data);
    gl_Position = mvp * vec4(vp, 1);

    TBN = mat3(vec3(tangent), vec3(bitangent), vec3(normal));
    Tex = vec2(decodeTexture(data) + tex) * normalizedSpriteSize;
    Pos = vec3(vp);
    Normal = normal;
    BlockLight = decodeLight(light) / maxLightValue;
    ViewSpacePos = (view * vec4(vp, 1)).xyz;
    LightSpacePos = (lightView * vec4(vp, 1)).xyz;
    aoFactor = AmbientOcclusion(ao, gl_VertexID);
}