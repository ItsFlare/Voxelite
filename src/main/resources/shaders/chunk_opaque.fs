#version 410

layout(location = 0) out vec3 color;
layout(location = 1) out vec3 normal;
layout(location = 2) out vec3 mer;
layout(location = 3) out vec3 bloom;

in vec2 Tex;
in vec3 Pos;
in vec3 BlockLight;
in vec3 LightSpacePos;
in vec3 ViewSpacePos;
in float aoFactor;

uniform sampler2DArray atlas;
uniform vec3 camera;
uniform mat4 view;

uniform bool shadows;

#include "include\csm.glsl"
#include "include\fog.glsl"
#include "include\light.glsl"
#include "include\normal.glsl"

void main() {

    vec3 n = GetNormal(Tex);
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

    normal = (view * vec4(n, 0)).xyz;
    mer = texture(atlas, vec3(Tex, 2)).rgb;
    bloom = BlockLight * t.rgb * mer.g; //TODO Perhaps BlockLight shouldn't be factored in?
}