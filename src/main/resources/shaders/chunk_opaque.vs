#version 410

#define WIDTH_EXP 4
#define HEIGHT_EXP 4

//const int Y_SHIFT = WIDTH_EXP + 1;
//const int X_SHIFT = Y_SHIFT + HEIGHT_EXP + 1;
//
//const int Z_MASK = (1 << (WIDTH_EXP + 1)) - 1;
//const int Y_MASK = ((1 << (HEIGHT_EXP + 1)) - 1) << Y_SHIFT;
//const int X_MASK = Z_MASK << X_SHIFT;

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
    uint x = data >> 28;
    uint y = (data & uint(0xf000000)) >> 24;
    uint z = (data & uint(0xf00000)) >> 20;
    uint u = (data & uint(0xff000)) >> 12;
    uint v = (data & uint(0xff0)) >> 4;
    vec3 vp = vec3(chunk) + pos + vec3(x, y, z);
    gl_Position = mvp * vec4(vp, 1);

    Tex = vec2(ivec2(u, v) + tex) * normalizedSpriteSize;
    Pos = vp;
    Normal = normal;
}