#version 430

uniform sampler2D composite;
uniform sampler2D bloom;

uniform bool aaEnabled;
uniform bool bloomEnabled;
uniform bool hdrEnabled;
uniform bool gammaEnabled;

uniform float gamma;
uniform float exposure;
uniform float bloomIntensity;

out vec4 fragColor;

#include "include\fxaa.glsl"

void main() {
    fragColor = texture(composite, pixel);

    if (aaEnabled) fxaa();

    if (bloomEnabled) {
        vec4 b = textureLod(bloom, pixel, 1);
        fragColor += b * b.a * bloomIntensity;
    }

    if (hdrEnabled) fragColor = vec4(1.0) - exp(-fragColor * exposure);

    if (gammaEnabled) fragColor = pow(fragColor, vec4(1.0 / gamma));
}
