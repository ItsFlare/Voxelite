#version 430
#extension GL_ARB_shading_language_include : require
#include "csm.glsl"
#include "ssr.glsl"
#include "fog.glsl"
#include "light.glsl"

#ifndef DEFERRED
    #include "deferred.glsl"
#endif

in vec2 Tex;
in vec3 Pos;
flat in ivec3 Normal;
in vec3 ViewNormal;
in vec3 BlockLight;
in vec3 LightSpacePos;
in vec3 ViewSpacePos;
flat in mat3 TBN;
in float aoFactor;

out vec4 FragColor;

uniform sampler2DArray atlas;
uniform vec3 camera;

uniform bool shadows;
uniform bool normalMapSet;

void main() {
    vec3 n;
    if(normalMapSet) {
        n = texture(atlas, vec3(Tex, 1)).rgb;
        n = n * 2 - 1;
        n = normalize(TBN * n);
    } else {
        n = Normal;
    }

    vec4 t = texture(atlas, vec3(Tex, 0));
    vec3 debugColor;
    float shadow = shadows ? ShadowCalculation(LightSpacePos, Normal, debugColor) : 1;
    FragColor = vec4(DirectionalLight(n, normalize(camera - Pos), shadow) + BlockLight, 1) * t;
    FragColor.rgb *= aoFactor;

    if(fogSet) {
        FragColor.rgb = mix(FragColor.rgb, fogColor, getFogFactor(ViewSpacePos));
    }

    if(reflections) {
        float roughness = texture(atlas, vec3(Tex, 2)).b;
        CalculateReflection(ViewSpacePos, ViewNormal, roughness, FragColor);
    }

    if(cascadeDebug) FragColor.rgb += debugColor;
}