#version 400 core
layout(points) in;
layout(triangle_strip, max_vertices = 4) out;

in uint[] Data;
in uint[] Light;
in uint[] AO;
in uint[] BYTEINDEX;
in uint[] BITINDEX;

out vec2 Tex;
out vec3 Pos;
flat out ivec3 Normal;
out vec4 BlockLight;
out vec3 LightSpacePos;
out vec4 eyeSpacePosition;
out float aoFactor;

uniform mat4 mvp;
uniform mat4 viewMatrix;
uniform ivec3 chunk;
uniform float normalizedSpriteSize;
uniform int maxLightValue;
uniform mat4 lightView;

uniform ivec3 normals[6];
uniform ivec3 vertices[6 * 4];
uniform ivec2 texCoords[6 * 4];

uniform int visibility;

const float[] aoMap = {0.25, 0.5, 0.75, 1};

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

    uint byteIndex = BYTEINDEX[0];
    uint byteShift = (byteIndex << 3);
    uint byteMask = 15 << byteShift;
    uint aoByte = (ao & byteMask) >> byteShift;

    uint bitIndex = BITINDEX[0];
    uint bitShift = (bitIndex << 1);
    uint bitMask = 3 << bitShift;
    uint aoBit = (aoByte & bitMask) >> bitShift;

    uint light = Light[0];
    BlockLight = vec4(light >> 20, (light >> 10) & uint(0x3ff), light & uint(0x3ff), 0) / maxLightValue;
    Normal = normals[d];
    aoFactor = aoMap[aoBit];

    ivec3 blockPos = chunk + ivec3(x, y, z);
    for(int i = 0; i < 4; i++) {
        ivec3 vp = blockPos + vertices[(4 * d) + i];
        gl_Position = mvp * vec4(vp, 1);
        Tex = vec2(ivec2(u, v) + texCoords[4 * d + i]) * normalizedSpriteSize;
        Pos = vec3(vp);
        LightSpacePos = (lightView * vec4(vp, 1)).xyz;
        eyeSpacePosition = viewMatrix * vec4(vp, 1);
        EmitVertex();
    }


    EndPrimitive();
}