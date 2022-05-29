#version 410

in vec3 pos;
in uint data;

uniform mat4 mvp;
uniform ivec3 chunk;

void main() {
    uint x = data >> 27;
    uint y = (data >> 22) & uint(0x1f);
    uint z = (data >> 17) & uint(0x1f);

    uint u = (data >> 8) & uint(0xff);
    uint v = data & uint(0xff);

    vec3 vp = vec3(chunk) + pos + vec3(x, y, z);
    gl_Position = mvp * vec4(vp, 1);
}