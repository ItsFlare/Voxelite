#version 410
#extension GL_ARB_shading_language_include : require
#include "include\csm.glsl"
#include "include\fog.glsl"
#include "include\light.glsl"

layout(location = 0) out vec3 color;
layout(location = 1) out vec3 normal;
layout(location = 2) out vec3 mer;

in vec2 Tex;
in vec3 Pos;
flat in ivec3 Normal;
in vec3 BlockLight;
in vec3 LightSpacePos;
in vec3 ViewSpacePos;
flat in mat3 TBN;
in float aoFactor;

uniform sampler2DArray atlas;
uniform vec3 camera;
uniform mat4 view;

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
    if(t.a < 0.0001) discard;

    vec3 debugColor;
    float shadow = shadows ? ShadowCalculation(LightSpacePos, Normal, debugColor) : 1;
    vec3 l = DirectionalLight(n, normalize(camera - Pos), shadow) + BlockLight;
    color = l * t.rgb * aoFactor;

    if(fogSet) {
        color = mix(color, fogColor, getFogFactor(ViewSpacePos));
    }

    if(cascadeDebug) color += debugColor;

    normal = (view * vec4(Normal, 0)).xyz;
    mer = texture(atlas, vec3(Tex, 2)).rgb;
}