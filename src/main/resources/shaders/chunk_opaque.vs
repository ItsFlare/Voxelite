#version 410

#define WIDTH_EXP 4
#define HEIGHT_EXP 4

const int Y_SHIFT = WIDTH_EXP + 1;
const int X_SHIFT = Y_SHIFT + HEIGHT_EXP + 1;

const int Z_MASK = (1 << (WIDTH_EXP + 1)) - 1;
const int Y_MASK = ((1 << (HEIGHT_EXP + 1)) - 1) << Y_SHIFT;
const int X_MASK = Z_MASK << X_SHIFT;

in int pos;
in vec2 tex;

out vec2 Tex;

uniform mat4 mvp;
uniform ivec3 chunk;

void main() {
    ivec3 v = ivec3((pos & X_MASK) >> X_SHIFT, (pos & Y_MASK) >> Y_SHIFT, pos & Z_MASK);
    gl_Position = mvp * vec4(vec3(chunk + v), 1);

    Tex = tex;
}