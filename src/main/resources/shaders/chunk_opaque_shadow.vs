#version 410

in ivec3 pos;
in uint data;

uniform mat4 mvp;
uniform ivec3 chunk;

void main() {
    uint x = data >> 27;
    uint y = (data >> 22) & uint(0x1f);
    uint z = (data >> 17) & uint(0x1f);

    ivec3 vp = chunk + pos + ivec3(x, y, z);
    gl_Position = mvp * vec4(vp, 1);
}