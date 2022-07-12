#version 410
#extension GL_ARB_shading_language_include : require
#include "include\util.glsl"

in ivec3 pos;
in uint data;

uniform mat4 mvp;
uniform ivec3 chunk;

void main() {
    ivec3 vp = chunk + pos + decodePosition(data);
    gl_Position = mvp * vec4(vp, 1);
}