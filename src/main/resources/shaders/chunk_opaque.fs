#version 410
in vec2 Tex;
in vec3 Pos;
in vec3 Normal;
in vec4 BlockLight;
in vec4 LightSpacePos;

out vec4 FragColor;


uniform sampler2D atlas;
uniform sampler2DArrayShadow shadowMap;
uniform vec3 camera;

uniform float ambientStrength;
uniform float diffuseStrength;
uniform float specularStrength;
uniform int phongExponent;
uniform int shadows;

uniform struct Light {
    vec3 direction;
    vec3 color;
} light;

const int CASCADES = 4;
uniform struct Cascade {
    float far;
    vec4 scale;
} cascades[CASCADES];


uniform int kernel = 0;
float kernelFactor = 1 / pow(kernel * 2 + 1, 2);
const int debug = 0;

vec3 debugColor = vec3(0, 1, 1);

float ShadowCalculation(vec4 fragPosLightSpace) {
    int c;
    for(c = -1; c++ < CASCADES; ) {
        if(gl_FragCoord.z < cascades[c].far) {
            debugColor = vec3(c / float(CASCADES), 0, 0);
            break;
        }
    }

    //Effectively projection to shadow clip space
    fragPosLightSpace.xyz *= cascades[c].scale.xyz;
    fragPosLightSpace.z += cascades[c].scale.w;
    fragPosLightSpace.xyz = 0.5 * fragPosLightSpace.xyz + 0.5;

    float bias = 10 * max(0.0025 * (1.0 - dot(Normal, -light.direction)), 0.0001);
    fragPosLightSpace.z -= bias;

    fragPosLightSpace.w = fragPosLightSpace.z; //Depth
    fragPosLightSpace.z = c; //Layer

    vec2 texelSize = 1.0 / textureSize(shadowMap, 0).xy;
    float shadow = 0.0;
    for(int x = -kernel; x <= kernel; ++x) {
        for (int y = -kernel; y <= kernel; ++y) {
            shadow += texture(shadowMap, vec4(fragPosLightSpace.xy + vec2(x, y) * texelSize, fragPosLightSpace.zw));
        }
    }

    shadow *= kernelFactor;

    return shadow;
}
vec3 DirectionalLight(Light light, vec3 normal, vec3 viewDirection) {
    vec3 color = light.color.xyz;
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
    vec4 t = texture(atlas, Tex);
    FragColor = vec4(DirectionalLight(light, Normal, normalize(camera - Pos)), 1) * t + BlockLight * t;

    //if(debug == 1) FragColor += vec4(debugColor, 1);
}