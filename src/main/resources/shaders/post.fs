#version 430

uniform sampler2D composite;
uniform sampler2D bloom;
uniform sampler2D godrays;

uniform mat4 projection;

uniform bool aaEnabled;
uniform bool bloomEnabled;
uniform bool hdrEnabled;
uniform bool gammaEnabled;
uniform bool godraysEnabled;

uniform float gamma;
uniform float exposure;
uniform float bloomIntensity;

uniform int godrayBlurSamples;
uniform int godrayBlurLod;
uniform float godrayBlurStride;

uniform vec2 lightScreen;

out vec4 fragColor;

#include "include\fxaa.glsl"
#include "include\util.glsl"

void main() {
    fragColor = texture(composite, pixel);

    if (aaEnabled) fxaa();

    if (bloomEnabled) {
        vec4 b = textureLod(bloom, pixel, 1);
        fragColor += b * b.a * bloomIntensity;
    }

    if (godraysEnabled) {
        vec2 uv = pixel;
        vec2 delta = clampRay(pixel, lightScreen) * godrayBlurStride / godrayBlurSamples;

        vec3 accumulator = vec3(0);
        for (int i = 0; i < godrayBlurSamples; i++) {
            accumulator += textureLod(godrays, uv, godrayBlurLod).rgb;
            uv += delta;
        }

        accumulator /= godrayBlurSamples; // Average
        fragColor.rgb += accumulator;
    }

    if (hdrEnabled) fragColor = vec4(1.0) - exp(-fragColor * exposure);

    if (gammaEnabled) fragColor = pow(fragColor, vec4(1.0 / gamma));

    fragColor.a = 1;
}
