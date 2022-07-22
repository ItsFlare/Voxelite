#version 430

in vec2 Tex;
in vec3 Pos;
in vec3 ViewNormal;
in vec3 BlockLight;
in vec3 LightSpacePos;
in vec3 ViewSpacePos;
in float aoFactor;

layout(location = 0) out vec4 color;
layout(location = 1) out vec4 bloom;

uniform sampler2DArray atlas;
uniform vec3 camera;

uniform bool shadows;

#include "include\deferred.glsl"
#include "include\csm.glsl"
#include "include\ssr.glsl"
#include "include\fog.glsl"
#include "include\light.glsl"
#include "include\normal.glsl"

void main() {

    vec3 n = GetNormal(Tex);
    vec4 t = texture(atlas, vec3(Tex, 0));
    vec3 mer = texture(atlas, vec3(Tex, 2)).rgb;

    vec3 debugColor;
    float shadow = shadows ? ShadowCalculation(LightSpacePos, Normal, debugColor) : 1;
    color = vec4(DirectionalLight(n, normalize(camera - Pos), shadow) + BlockLight, 1) * t;
    color.rgb *= aoFactor;

    if(fogSet) {
        color.rgb = mix(color.rgb, fogColor, getFogFactor(ViewSpacePos));
    }
    debugColor = n;

    if(reflections) {
        float roughness = mer.b;
        CalculateReflection(ViewSpacePos, ViewNormal, roughness, color);
    }

    /*
    Fragment color F := color
    Emission e := mer.g
    Intensity i := 1 - F.a
    Correction c := 1 / max(F)
    Spectrum W := F * c

    Desired formula:  D = F * e + D * W * i
    GL blend formula: D = S * S.a + D * S
    => S := W * i
    => S.a := e / (c * i)
    */

    float emission = mer.g;
    float intensity = 1.0 - color.a;
    float correction = 1.0 / max(color.r, max(color.g, color.b));
    vec3 spectrum = color.rgb * correction;
    bloom = vec4(spectrum * intensity, emission / (correction * intensity));

    if(cascadeDebug) color.rgb += debugColor;
}