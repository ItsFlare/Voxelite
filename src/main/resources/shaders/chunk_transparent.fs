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
    Transmittance t := 1 - F.a
    Correction c := 1 / max(F)
    Spectrum W := F * c

    Desired formula:  D = F * e + D * W * t
    GL blend formula: D = S * S.a + D * S
    => S := W * t
    => S.a := e / (c * t)
    */

    float emission = mer.g;
    float transmittance = 1.0 - t.a;
    float correction = clamp(1.0 / max(t.r, max(t.g, t.b)), 0, 100);
    vec3 spectrum = t.rgb * correction;
    bloom = vec4(spectrum * transmittance, emission / (correction * transmittance));

    if(cascadeDebug) color.rgb += debugColor;

    //Cap alpha below 1 to allow other shaders to differentiate transparent geometry without depth information
    color.a = min(color.a, 0.995);
}