uniform sampler2DArrayShadow shadowMap;
uniform float constantBias;

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
uniform bool cascadeDebug;
float kernelFactor = 1 / pow(kernel * 2 + 1, 2);

float ShadowCalculation(vec3 fragPosLightSpace, vec3 normal, out vec3 debugColor) {
    int c;
    for(c = -1; c++ < MAX_CASCADES; ) {
        if(gl_FragCoord.z < cascades[c].far) {
            break;
        }
    }

    debugColor = cascadeDebug ? vec3(0, 0, c / float(MAX_CASCADES)) : vec3(0);

    //Effectively projection to shadow screen space
    fragPosLightSpace *= cascades[c].scale;
    fragPosLightSpace += cascades[c].translation;
    fragPosLightSpace = 0.5 * fragPosLightSpace + 0.5;

    vec2 textureSize = textureSize(shadowMap, 0).xy;
    vec2 texelSize = 1.0 / textureSize;

    float blocksPerPixel = 2 / (cascades[c].scale.y * textureSize.y);
    float depthPerBlock = cascades[c].scale.z * -2;
    float bias = tan(acos(min(abs(dot(normal, -light.direction)), 1))) * blocksPerPixel * depthPerBlock + constantBias;

    float shadow = 0.0;
    for(int x = -kernel; x <= kernel; ++x) {
        for (int y = -kernel; y <= kernel; ++y) {
            shadow += texture(shadowMap, vec4(fragPosLightSpace.xy + vec2(x, y) * texelSize, c, fragPosLightSpace.z - bias * max((abs(x) + abs(y)), 1))); //TODO Fix PCF overbiasing
        }
    }

    shadow *= kernelFactor;

    return shadow;
}