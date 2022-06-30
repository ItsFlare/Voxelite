#version 400 core
layout(points) in;
layout(triangle_strip, max_vertices = 4) out;

in uint[] Data;
in uint[] Light;
in uint[] AO;
in mat3[] inTBN;

out vec2 Tex;
out vec3 Pos;
flat out ivec3 Normal;
out vec3 BlockLight;
out vec3 LightSpacePos;
out vec3 ViewSpacePos;
flat out mat3 TBN;
out float aoFactor;

uniform mat4 mvp;
uniform mat4 view;
uniform ivec3 chunk;
uniform float normalizedSpriteSize;
uniform int maxLightValue;
uniform mat4 lightView;
uniform bool aoSet;

uniform ivec3 normals[6];
uniform ivec3 vertices[6 * 4];
uniform ivec2 texCoords[6 * 4];

uniform int visibility;

const vec4 aoMap = vec4(0.5, 0.7, 0.9, 1);

void main() {
    uint data = Data[0];
    uint d = data & uint(0x7);
    uint ao = AO[0];

    //Direction cull
    if((visibility & (1 << d)) == 0) return;

    uint x = data >> 27;
    uint y = (data >> 22) & uint(0x1f);
    uint z = (data >> 17) & uint(0x1f);

    uint u = (data >> 10) & uint(0x7f);
    uint v = (data >> 3) & uint(0x7f);

    uint light = Light[0];
    BlockLight = vec3(light >> 20, (light >> 10) & uint(0x3ff), light & uint(0x3ff)) / maxLightValue;
    Normal = normals[d];
    TBN = inTBN[0];

    ivec3 blockPos = chunk + ivec3(x, y, z);
    for(int i = 0; i < 4; i++) {
        ivec3 vp = blockPos + vertices[(4 * d) + i];
        gl_Position = mvp * vec4(vp, 1);
        Tex = vec2(ivec2(u, v) + texCoords[4 * d + i]) * normalizedSpriteSize;
        Pos = vec3(vp);
        LightSpacePos = (lightView * vec4(vp, 1)).xyz;
        ViewSpacePos = (view * vec4(vp, 1)).xyz;
        aoFactor = aoSet ? aoMap[(ao >> ((i & 3) << 1)) & 3u] : 1;
        aoFactor = min(aoFactor * 1.5, 1); //Reduce AO effect on transparent materials
        EmitVertex();
    }

    EndPrimitive();
}