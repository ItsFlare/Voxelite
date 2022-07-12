#version 410
#include "include\compression.glsl"

in ivec3 pos;
in uint data;

uniform mat4 mvp;
uniform ivec3 chunk;

void main() {
    ivec3 vp = chunk + pos + decodePosition(data);
    gl_Position = mvp * vec4(vp, 1);
}