#version 430
#extension GL_ARB_shading_language_include : require
#include "csm.glsl"
#include "ssr.glsl"
#include "fog.glsl"

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

uniform float ambientStrength;
uniform float diffuseStrength;
uniform float specularStrength;
uniform int phongExponent;
uniform bool shadows;
uniform bool normalMapSet;


vec3 DirectionalLight(vec3 normal, vec3 viewDirection, float shadow) {
    vec3 color = light.color.rgb;
    vec3 lightDirection = normalize(-light.direction);

    //diffuse
    float diff = max(dot(normal, lightDirection), 0.0);

    //specular (blinn)
    vec3 halfwayDirection = normalize(lightDirection + viewDirection);
    float spec = pow(max(dot(normal, halfwayDirection), 0.0), phongExponent);

    vec3 ambient  = color * ambientStrength;
    vec3 diffuse  = color * diffuseStrength * diff * shadow;
    vec3 specular = color * specularStrength * spec * shadow;
    return (ambient + diffuse + specular);
}

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