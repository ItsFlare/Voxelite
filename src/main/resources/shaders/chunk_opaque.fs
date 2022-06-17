#version 410
layout(location = 0) out vec4 color;
layout(location = 1) out vec3 normal;
layout(location = 2) out vec3 mer;

in vec2 Tex;
in vec3 Pos;
flat in ivec3 Normal;
in vec3 BlockLight;
in vec3 LightSpacePos;

uniform sampler2DArray atlas;
uniform sampler2DArrayShadow shadowMap;
uniform vec3 camera;

uniform float ambientStrength;
uniform float diffuseStrength;
uniform float specularStrength;
uniform float constantBias;
uniform int phongExponent;
uniform int shadows;
uniform mat4 view;

uniform struct Light {
    vec3 direction;
    vec3 color;
} light;

const int MAX_CASCADES = 4;
uniform struct Cascade {
    float far;
    vec3 scale;
    vec3 translation;
} cascades[MAX_CASCADES];


uniform int kernel;
uniform int cascadeDebug;
float kernelFactor = 1 / pow(kernel * 2 + 1, 2);

vec3 debugColor;

float ShadowCalculation(vec3 fragPosLightSpace) {
    int c;
    for(c = -1; c++ < MAX_CASCADES; ) {
        if(gl_FragCoord.z < cascades[c].far) {
            break;
        }
    }

    debugColor = vec3(0, 0, c / float(MAX_CASCADES));

    //Effectively projection to shadow screen space
    fragPosLightSpace *= cascades[c].scale;
    fragPosLightSpace += cascades[c].translation;
    fragPosLightSpace = 0.5 * fragPosLightSpace + 0.5;

    vec2 textureSize = textureSize(shadowMap, 0).xy;
    vec2 texelSize = 1.0 / textureSize;

    float blocksPerPixel = 2 / (cascades[c].scale.y * textureSize.y);
    float depthPerBlock = cascades[c].scale.z * -2;
    float bias = tan(acos(min(abs(dot(Normal, -light.direction)), 1))) * blocksPerPixel * depthPerBlock + constantBias;

    float shadow = 0.0;
    for(int x = -kernel; x <= kernel; ++x) {
        for (int y = -kernel; y <= kernel; ++y) {
            shadow += texture(shadowMap, vec4(fragPosLightSpace.xy + vec2(x, y) * texelSize, c, fragPosLightSpace.z - bias * max((abs(x) + abs(y)), 1))); //TODO Fix PCF overbiasing
        }
    }

    shadow *= kernelFactor;

    return shadow;
}
vec3 DirectionalLight(vec3 normal, vec3 viewDirection) {
    vec3 color = light.color.rgb;
    vec3 lightDirection = normalize(-light.direction);

    //diffuse
    float diff = max(dot(normal, lightDirection), 0.0);

    //specular (blinn)
    vec3 halfwayDirection = normalize(lightDirection + viewDirection);
    float spec = pow(max(dot(normal, halfwayDirection), 0.0), phongExponent);

    float shadow = shadows == 1 ? ShadowCalculation(LightSpacePos) : 1;
    vec3 ambient  = color * ambientStrength;
    vec3 diffuse  = color * diffuseStrength * diff * shadow;
    vec3 specular = color * specularStrength * spec * shadow;
    return (ambient + diffuse + specular);
}

void main() {
    vec3 albedo = texture(atlas, vec3(Tex, 0)).rgb;
    //vec3 normal = texture(atlas, vec3(Tex, 1)).xyz;
    vec3 n = Normal;

    color = vec4((DirectionalLight(n, normalize(camera - Pos)) + BlockLight) * albedo, gl_FragDepth);
    if(cascadeDebug == 1) color.xyz += debugColor;

    normal = 0.5 * (view * vec4(n, 0)).xyz + 0.5;
    mer = texture(atlas, vec3(Tex, 2)).rgb;
}