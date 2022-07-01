#version 430

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

uniform sampler2D depth;
uniform sampler2D opaque;
uniform float debugRoughness;
uniform mat4 projection;

uniform sampler2DArray atlas;
uniform sampler2DArrayShadow shadowMap;
uniform vec3 camera;

uniform float ambientStrength;
uniform float diffuseStrength;
uniform float specularStrength;
uniform float constantBias;
uniform int phongExponent;
uniform bool shadows;
uniform bool normalMapSet;
uniform bool fogSet;
uniform int fogRange;

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

    float shadow = shadows ? ShadowCalculation(LightSpacePos) : 1;
    vec3 ambient  = color * ambientStrength;
    vec3 diffuse  = color * diffuseStrength * diff * shadow;
    vec3 specular = color * specularStrength * spec * shadow;
    return (ambient + diffuse + specular);
}

float getFogFactor(float fogCoordinate) {
    float density = 0.015;
    float start = fogRange - 10;
    float end = fogRange + 30;

    if (fogCoordinate < start) {
        return 0;
    } else {

        return clamp((fogCoordinate - start) / (end - start), 0, 1);
    }
}


uniform bool reflections;
uniform bool coneTracing;

const int SEARCH_STEPS = 25;
const int MAX_STEPS = 200;
const float RAY_STRIDE = 1;
const int CONE_STEPS = 8;

vec2 viewport = textureSize(opaque, 0);
float aspectRatio = viewport.x / viewport.y;

float LinearDepth(float depth) {
    depth = depth * 2 - 1; //Convert to NDC
    float a = projection[2][2];
    float b = projection[3][2];
    float z_view = b / (a + depth);

    return z_view;
}

float SampleDepth(vec2 uv) {
    return LinearDepth(texture(depth, uv).x);
}

//vec3 toViewSpace(vec2 screen) {
//    return texture(position, screen).xyz;
//}

vec3 toViewSpace(vec2 uv) {
    float z = SampleDepth(uv);

    uv = uv * 2 - 1; //Convert to NDC
    vec3 viewPos = vec3(0);
    viewPos.x = z * uv.x / projection[0][0];
    viewPos.y = z * uv.y / projection[1][1];
    viewPos.z = -z;

    return viewPos;
}

vec2 toScreenSpace(vec3 view) {
    vec4 projected = projection * vec4(view, 1);
    projected.xy /= projected.w;
    projected.xy = projected.xy * 0.5 + 0.5;
    return projected.xy;
}

vec2 search(vec3 position, vec3 direction) {
    for (int i = 0; i < SEARCH_STEPS; i++){
        direction *= 0.8;

        float z = -SampleDepth(toScreenSpace(position));

        position += sign(z - position.z) * direction;
    }

    return toScreenSpace(position);
}

bool rayMarchView(vec3 position, vec3 direction, out vec2 hitPixel) {
    direction *= RAY_STRIDE;

    for (int i = 0; i < MAX_STEPS; i++) {
        position += direction;
        vec2 ss = toScreenSpace(position).xy;
        if (ss.y < 0 || ss.y > 1) break;
        if (ss.x < 0 || ss.x > 1) break;

        float z = -SampleDepth(ss);

        if (position.z <= z) {
            hitPixel = search(position, direction);
            //hitPixel = ss;
            return true;
        }
    }

    //Pretend our ray converged to infinity
    hitPixel = toScreenSpace(position).xy;
    return true;
}

/*
Uludag. Hi-z screen-space cone-traced reflections. 2014.
*/

float CalculateConeAngle(float roughness) {
    float a = 2 + (16 - 2) * roughness;
    return cos(0.244 / (a + 1));
}

float CalculateIsoscelesTriangleBase(float angle, float rayLength) {
    return 2 * rayLength * tan(angle);
}

float CalculateIsoscelesInRadius(float base, float rayLength) {
    return (base * (sqrt(base * base + 4 * rayLength * rayLength) - base)) / (4 * rayLength);
}

float CalculateFade(vec2 hit) {
    float I_end = 1;
    float I_start = 0.7;
    float D_boundary = length(hit - vec2(0.5)) * 2;
    float f_border = clamp((D_boundary - I_start) / (I_end - I_start), 0.0, 1.0);

    return f_border;
}

vec4 ConeTrace(vec2 rayOrigin, vec2 rayDirection, float roughness) {
    vec4 color = vec4(0);
    vec2 rayDirectionNormalized = normalize(rayDirection);
    float rayLength = length(rayDirection);

    float glossiness = 1 - roughness;
    int maxLevel = textureQueryLevels(opaque);

    for (int i = 0; i < CONE_STEPS; i++) {
        float angle = CalculateConeAngle(roughness);
        float base = CalculateIsoscelesTriangleBase(angle, rayLength);
        float radius = CalculateIsoscelesInRadius(base, rayLength);

        vec2 pos = rayOrigin + rayDirectionNormalized * (rayLength - radius);
        float level = clamp(log2(radius * max(viewport.x, viewport.y)), 0, maxLevel);

        color += vec4(textureLod(opaque, pos.xy, level).rgb * glossiness, glossiness);
        rayLength -= 2 * radius;
        glossiness *= glossiness;
    }

    return color / color.a;
}

/* ----- */

void main() {
    vec3 n;
    if(normalMapSet) {
        n = texture(atlas, vec3(Tex,1)).rgb;
        n = n * 2 - 1;
        n = normalize(TBN * n);
    } else {
        n = Normal;
    }

    vec4 t = texture(atlas, vec3(Tex, 0));
    FragColor = vec4(DirectionalLight(n, normalize(camera - Pos)) + BlockLight, 1) * t;
    FragColor.rgb *= aoFactor;

    if(fogSet) {
        float fogCoordinate = length(ViewSpacePos.xyz);
        vec4  fogColor = vec4(0.4, 0.4, 0.4, 1.0);
        FragColor = mix(FragColor, fogColor, getFogFactor(fogCoordinate));
    }

    if(cascadeDebug) FragColor += vec4(debugColor, 1);

    if(reflections) {
        vec2 pixel = gl_FragCoord.xy / viewport;
        vec4 o = texture(opaque, pixel);
        o.a = 1;

        vec3 mer = texture(atlas, vec3(Tex, 2)).rgb;
        float roughness = clamp(mer.b + debugRoughness, 0, 1);

        vec3 rayDirection = reflect(normalize(ViewSpacePos), normalize(ViewNormal));

        if (dot(vec3(0, 0, -1), rayDirection) > 0) {
            vec2 hitPixel;
            bool hit = rayMarchView(ViewSpacePos, rayDirection, hitPixel);

            if (hit) {
                vec4 reflectionColor = (coneTracing && roughness < 1) ? ConeTrace(hitPixel, hitPixel - toScreenSpace(ViewSpacePos), roughness) : vec4(texture(opaque, hitPixel).rgb, 1);
                FragColor = mix(mix(reflectionColor, FragColor, roughness), FragColor, CalculateFade(hitPixel.xy));
                o = mix(o, vec4(1, 0, 0, 1), 0.1);
            }
        }
    }
}