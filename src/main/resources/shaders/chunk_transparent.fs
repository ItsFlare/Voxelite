#version 430

in vec2 Tex;
in vec3 Pos;
in vec3 ViewNormal;
in vec3 BlockLight;
in vec3 LightSpacePos;
in vec3 ViewSpacePos;
in float aoFactor;

out vec4 FragColor;

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

    vec3 debugColor;
    float shadow = shadows ? ShadowCalculation(LightSpacePos, Normal, debugColor) : 1;
    FragColor = vec4(DirectionalLight(n, normalize(camera - Pos), shadow) + BlockLight, 1) * t;
    FragColor.rgb *= aoFactor;

    if(fogSet) {
        FragColor.rgb = mix(FragColor.rgb, fogColor, getFogFactor(ViewSpacePos));
    }
    debugColor = n;

    if(reflections) {
        float roughness = texture(atlas, vec3(Tex, 2)).b;
        CalculateReflection(ViewSpacePos, ViewNormal, roughness, FragColor);
    }

    if(cascadeDebug) FragColor.rgb += debugColor;
}