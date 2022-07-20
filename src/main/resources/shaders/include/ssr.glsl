#include "include\ss.glsl"

uniform float debugRoughness;
uniform bool reflections;
uniform bool coneTracing;

const int SEARCH_STEPS = 25;
const int MAX_STEPS = 200;
const float RAY_STRIDE = 1;
const int CONE_STEPS = 8;

vec2 search(vec3 position, vec3 direction) {
    for (int i = 0; i < SEARCH_STEPS; i++){
        direction *= 0.8;

        float z = -SampleDepth(toScreenSpace(position));

        position += sign(z - position.z) * direction;
    }

    return toScreenSpace(position);
}

bool raymarchView(vec3 position, vec3 direction, out vec2 hitPixel) {
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
Mara and McGuire. Efficient GPU Screen-Space Ray Tracing. 2014.
        Overhauled reference implementation (BSD 2)
*/

void swap(in out float a, in out float b) {
    float temp = a;
    a = b;
    b = temp;
}

bool raymarchScreen
(vec3           csOrigin,
vec3            csDirection,
mat4            projection,
float           nearPlane,
float           farPlane,
float           stride,
float           jitterFraction,
float           maxSteps,
in float        maxRayTraceDistance,
out vec2        hitPixel,
out vec3        csHitPoint,
out int stepCount) {

    // Clip ray to a near plane in 3D (doesn't have to be *the* near plane, although that would be a good idea)
    float rayLength = ((csOrigin.z + csDirection.z * maxRayTraceDistance) > nearPlane) ?
    (nearPlane - csOrigin.z) / csDirection.z :
    maxRayTraceDistance;
    vec3 csEndPoint = csDirection * rayLength + csOrigin;

    // Project into screen space
    vec4 H0 = projection * vec4(csOrigin, 1.0);
    vec4 H1 = projection * vec4(csEndPoint, 1.0);

    // There are a lot of divisions by w that can be turned into multiplications
    // at some minor precision loss...and we need to interpolate these 1/w values
    // anyway.
    //
    // Because the caller was required to clip to the near plane,
    // this homogeneous division (projecting from 4D to 2D) is guaranteed
    // to succeed.
    float k0 = 1.0 / H0.w;
    float k1 = 1.0 / H1.w;

    // Switch the original points to values that interpolate linearly in 2D
    vec3 Q0 = csOrigin * k0;
    vec3 Q1 = csEndPoint * k1;

    // Screen-space endpoints
    vec2 P0 = toPixelSpace(csOrigin);
    vec2 P1 = toPixelSpace(csEndPoint);

    // [Optional clipping to frustum sides here]

    // Initialize to off screen
    hitPixel = vec2(-1.0);

    // If the line is degenerate, make it cover at least one pixel
    // to avoid handling zero-pixel extent as a special case later
    P1 += vec2((dot(P0, P1) < 0.0001) ? 0.01 : 0.0);

    vec2 delta = P1 - P0;

    // Permute so that the primary iteration is in x to reduce
    // large branches later
    bool permute = false;
    if (abs(delta.x) < abs(delta.y)) {
        // More-vertical line. Create a permutation that swaps x and y in the output
        permute = true;

        // Directly swizzle the inputs
        delta = delta.yx;
        P1 = P1.yx;
        P0 = P0.yx;
    }

    // From now on, "x" is the primary iteration direction and "y" is the secondary one
    float stepDirection = sign(delta.x);
    float invdx = stepDirection / delta.x;
    vec2 dP = vec2(stepDirection, invdx * delta.y);

    // Track the derivatives of Q and k
    vec3 dQ = (Q1 - Q0) * invdx;
    float dk = (k1 - k0) * invdx;

    // Scale derivatives by the desired pixel stride
    dP *= stride;
    dQ *= stride;
    dk *= stride;

    // Offset the starting values by the jitter fraction
    P0 += dP * jitterFraction;
    Q0 += dQ * jitterFraction;
    k0 += dk * jitterFraction;

    // Slide P from P0 to P1, (now-homogeneous) Q from Q0 to Q1, and k from k0 to k1
    vec3 Q = Q0;
    float k = k0;

    // P1.x is never modified after this point, so pre-scale it by
    // the step direction for a signed comparison
    float end = P1.x * stepDirection;

    stepCount = 0;

    // We only advance the z field of Q in the inner loop, since
    // Q.xy is never used until after the loop terminates.
    for (vec2 P = P0;
    ((P.x * stepDirection) <= end) &&
    (stepCount < maxSteps);
    P += dP, Q.z += dQ.z, k += dk, stepCount += 1) {

        hitPixel = permute ? P.yx : P;

        //TODO Move out of loop
        if (hitPixel.x < 0 || hitPixel.x >= viewport.x) break;
        if (hitPixel.y < 0 || hitPixel.y >= viewport.y) break;

        // Camera-space z of the background
        float sceneZ = -SampleDepth(toScreenSpace(hitPixel));
        float rayZ = Q.z * (1.0 / k);

        //TODO Move out of loop
        if (rayZ > 0) break;

        if (rayZ <= sceneZ || rayZ < farPlane) {
            Q.xy += dQ.xy * stepCount;
            csHitPoint = Q * (1.0 / k);
            return true;
        }

        //        //Debug hit
        //        if (length(hitPixel.xy - gl_FragCoord.xy) <= 1) {
        //            Q.xy += dQ.xy * stepCount;
        //            csHitPoint = Q * (1.0 / k);
        //            return true;
        //        }
    }

    return false;
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

    float glossiness = 1 - min(roughness, 0.999); //TODO Improve singularity fix
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

void CalculateReflection(vec3 viewPos, vec3 normal, in float roughness, inout vec4 color) {
    roughness = clamp(roughness + debugRoughness, 0, 1);

    vec3 rayDirection = reflect(normalize(viewPos), normalize(normal));

    if (dot(vec3(0, 0, -1), rayDirection) > 0) {
        bool hit;
        vec2 hitPixel;

        bool dda = false;
        if(dda) {
            //TODO Extract magic values

            vec3 hitPoint;
            int steps;
            int maxSteps = 5000;
            int range = 1000;

            hit = raymarchScreen(viewPos, rayDirection, projection, -0.2f, -500, 1, 0, maxSteps, range, hitPixel, hitPoint, steps);
            hitPixel = toScreenSpace(hitPixel);
        } else {
            hit = raymarchView(viewPos, rayDirection, hitPixel);
        }

        if (hit) {
            vec4 reflectionColor = coneTracing ? ConeTrace(hitPixel, hitPixel - toScreenSpace(viewPos), roughness) : texture(opaque, hitPixel);
            reflectionColor.a = 1;

            color = mix(mix(reflectionColor, color, roughness), color, CalculateFade(hitPixel));
        }
    }
}