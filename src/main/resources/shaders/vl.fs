#version 430

uniform sampler2D composite;
uniform sampler2D depth;
uniform sampler2D noise;

out vec3 vl;

vec2 viewport = textureSize(composite, 0);

#include "include\godrays.glsl"

void main() {
    vec2 screenSpace = gl_FragCoord.xy / viewport;
    vl = CalculateGodrays(screenSpace);
}