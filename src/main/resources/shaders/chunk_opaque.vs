#version 410

in ivec2 tex;
in vec3 pos;
in vec3 normal;
in uint data;

out vec2 Tex;
out vec3 Pos;
out vec3 Normal;

uniform mat4 mvp;
uniform ivec3 chunk;
uniform float normalizedSpriteSize;

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
}