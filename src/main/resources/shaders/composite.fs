#version 430

in vec2 pixel;

out vec4 FragColor;

uniform float debugRoughness;
uniform sampler2D opaque;
uniform sampler2D normal;
uniform sampler2D mer;
uniform sampler2D depth;
uniform mat4 projection;

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
    //Copy depth into default framebuffer
    gl_FragDepth = texture(depth, pixel).x;

    vec4 o = texture(opaque, pixel);
    o.a = 1;

    vec3 n = texture(normal, pixel).xyz;

    //No geometry (background)
    if (n == vec3(0)) {
        FragColor = o;
        return;
    }

    if(reflections) {
        vec3 mer = texture(mer, pixel).rgb;
        float roughness = clamp(mer.b + debugRoughness, 0, 1);

        vec3 viewPos = toViewSpace(pixel);
        vec3 rayDirection = reflect(normalize(viewPos), normalize(n));

        if (dot(vec3(0, 0, -1), rayDirection) > 0) {
            vec2 hitPixel;
            bool hit = rayMarchView(viewPos, rayDirection, hitPixel);

            if (hit) {
                vec4 reflectionColor = (coneTracing && roughness < 1) ? ConeTrace(hitPixel, hitPixel - toScreenSpace(viewPos), roughness) : vec4(texture(opaque, hitPixel).rgb, 1);
                o = mix(mix(reflectionColor, o, roughness), o, CalculateFade(hitPixel.xy));
                //o = mix(o, vec4(1, 0, 0, 1), 0.1);
            }
        }
    }

    FragColor = o;

    //FragColor = mix(vec4(rayDirection, 1), FragColor, 0.00001);
    //FragColor = mix(vec4(toScreenSpace(toViewSpace(pixel)), 1, 1), FragColor, 0.00001);
    //FragColor = mix(vec4(toViewSpaceReconstruct(pixel) - toViewSpace(pixel), 1) / 100, FragColor, 0.00001);
    //FragColor = mix(vec4(SampleDepth(pixel) / 100), FragColor, 0.00001);
    //FragColor = mix(vec4(rayDirection, 1), FragColor, 0.00001);
    //FragColor = mix(vec4(n, 1), FragColor, 0.00001);
}