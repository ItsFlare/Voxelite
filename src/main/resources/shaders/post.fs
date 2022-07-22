#version 430

uniform sampler2D composite;
uniform sampler2D bloom;
uniform sampler2D vl;

uniform bool aaEnabled;
uniform bool bloomEnabled;
uniform bool hdrEnabled;
uniform bool gammaEnabled;
uniform bool godraysEnabled = true;

uniform float gamma;
uniform float exposure;
uniform float bloomIntensity;

uniform int godraySamples = 5;
uniform int godrayLod = 2;
uniform float godrayStride = 0.01;

const vec2 sun = vec2(0.5);

out vec4 fragColor;

#include "include\fxaa.glsl"

void main() {
    fragColor = texture(composite, pixel);

    if (aaEnabled) fxaa();

    if (bloomEnabled) {
        vec4 b = textureLod(bloom, pixel, 1);
        fragColor += b * b.a * bloomIntensity;
    }

    if (godraysEnabled) {
        vec2 uv = pixel;
        vec2 delta = (sun - pixel) * godrayStride;

        float accumulator = 0;
        for (int i = 0; i < godraySamples; i++) {
            accumulator += textureLod(composite, uv, godrayLod).a;
            uv += delta;
        }

        fragColor += accumulator / godraySamples;
    }

    if (hdrEnabled) fragColor = vec4(1.0) - exp(-fragColor * exposure);

    if (gammaEnabled) fragColor = pow(fragColor, vec4(1.0 / gamma));

    fragColor.a = 1;
}
