#version 400 core
#include "include\compression.glsl"
#include "include\ao.glsl"

layout(points) in;
layout(triangle_strip, max_vertices = 4) out;

in uint[] Data;
in uint[] Light;
in uint[] AO;
in mat3[] inTBN;

out vec2 Tex;
out vec3 Pos;
flat out ivec3 Normal;
out vec3 ViewNormal;
out vec3 BlockLight;
out vec3 LightSpacePos;
out vec3 ViewSpacePos;
flat out mat3 TBN;

uniform mat4 mvp;
uniform mat4 view;
uniform ivec3 chunk;
uniform float normalizedSpriteSize;
uniform int maxLightValue;
uniform mat4 lightView;

uniform ivec3 normals[6];
uniform ivec3 vertices[6 * 4];
uniform ivec2 texCoords[6 * 4];

uniform int visibility;

void main() {
    uint data = Data[0];
    uint direction = data & uint(0x7);

    //Direction cull
    if((visibility & (1 << direction)) == 0) return;

    uint ao = AO[0];
    ivec3 blockPos = chunk + decodePosition(data);
    vec3 blockLight = decodeLight(Light[0]) / maxLightValue;
    ivec3 normal = normals[direction];
    vec3 viewNormal = (view * vec4(normal, 0)).xyz;

    for(int i = 0; i < 4; i++) {
        //Per quad
        BlockLight = blockLight;
        Normal = normal;
        ViewNormal = viewNormal;
        TBN = inTBN[0];

        //Per vertex
        ivec3 vp = blockPos + vertices[(4 * direction) + i];
        gl_Position = mvp * vec4(vp, 1);
        Tex = vec2(decodeTexture(data) + texCoords[4 * direction + i]) * normalizedSpriteSize;
        Pos = vec3(vp);
        LightSpacePos = (lightView * vec4(vp, 1)).xyz;
        ViewSpacePos = (view * vec4(vp, 1)).xyz;
        aoFactor = AmbientOcclusion(ao, i);
        aoFactor = min(aoFactor * 1.5, 1); //Reduce AO effect on transparent materials

        EmitVertex();
    }

    EndPrimitive();
}