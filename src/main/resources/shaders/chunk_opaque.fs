#version 410
in vec2 Tex;
in vec3 Pos;
in vec3 Normal;
in vec4 BlockLight;
in vec4 LightSpacePos;

out vec4 FragColor;


uniform sampler2D atlas;
uniform sampler2D shadowMap;
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

const int KERNEL = 1;
const float KERNEL_FACTOR = 1 / pow(KERNEL * 2 + 1, 2);

float ShadowCalculation(vec4 fragPosLightSpace)
{
    // perform perspective divide
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    // transform to [0,1] range
    projCoords = projCoords * 0.5 + 0.5;
    // get closest depth value from light's perspective (using [0,1] range fragPosLight as coords)
    // float closestDepth = texture(shadowMap, projCoords.xy).r;
    // get depth of current fragment from light's perspective
    float currentDepth = projCoords.z;
    // calculate bias (based on depth map resolution and slope)
    float bias = 10 * max(0.0025 * (1.0 - dot(Normal, -light.direction)), 0.0001);
    // check whether current frag pos is in shadow
    // float shadow = currentDepth - bias > closestDepth  ? 1.0 : 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);

    // PCF
    float shadow = 0.0;
    for(int x = -KERNEL; x <= KERNEL; ++x) {
        for(int y = -KERNEL; y <= KERNEL; ++y) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth  ? 1.0 : 0.0;
        }
    }

    shadow *= KERNEL_FACTOR;

    // keep the shadow at 0.0 when outside the far_plane region of the light's frustum.
    if(projCoords.z > 1.0) shadow = 0.0;

    return 1 - shadow;
}
vec3 DirectionalLight(Light light, vec3 normal, vec3 viewDirection)
{
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
}