vec2 viewport = textureSize(opaque, 0);
float aspectRatio = viewport.x / viewport.y;

float LinearDepth(float depth) {
    depth = depth * 2 - 1;//Convert to NDC
    float a = projection[2][2];
    float b = projection[3][2];
    float z_view = b / (a + depth);

    return z_view;
}

float SampleDepth(vec2 uv) {
    return LinearDepth(texture(depth, uv).x);
}

vec3 toViewSpace(vec2 uv) {
    float z = SampleDepth(uv);

    uv = uv * 2 - 1;//Convert to NDC
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

vec2 toScreenSpace(vec2 pixelSpace) {
    return pixelSpace / viewport;
}

vec2 toPixelSpace(vec2 screenSpace) {
    return vec2(screenSpace * viewport);
}

vec2 toPixelSpace(vec3 viewSpace) {
    return toPixelSpace(toScreenSpace(viewSpace));
}

//const float MAX_FLOAT = intBitsToFloat(2139095039);
//bool raymarch(vec2 origin, vec2 target, vec2 viewport) {
//    ivec2 pixel = ivec2(gl_FragCoord.xy);
//
//    float alpha = 1.0E-6;
//    origin = mix(origin, target, alpha);
//    target = mix(target, origin, alpha);
//
//    float lenX = target.x - origin.x;
//    float lenY = target.y - origin.y;
//
//    int signX = int(sign(lenX));
//    int signY = int(sign(lenY));
//    float tDeltaX = signX == 0 ? MAX_FLOAT : (signX / lenX);
//    float tDeltaY = signY == 0 ? MAX_FLOAT : (signY / lenY);
//
//    float tMaxX = tDeltaX * (signX > 0 ? 1 - fract(origin.x) : fract(origin.x));
//    float tMaxY = tDeltaY * (signY > 0 ? 1 - fract(origin.y) : fract(origin.y));
//
//    int x = int(floor(origin.x));
//    int y = int(floor(origin.y));
//
//    int i = 0;
//
//    while (
//    x >= 0 && x <= viewport.x &&
//    y >= 0 && y <= viewport.y &&
//    (tMaxX <= 1 || tMaxY <= 1)
//    ) {
//
//        if (tMaxX < tMaxY) {
//            x += signX;
//            tMaxX += tDeltaX;
//        } else {
//            y += signY;
//            tMaxY += tDeltaY;
//        }
//
//        ivec2 pos = ivec2(x, y);
//        if (pixel == pos) return true;
//    };
//
//    return false;
//}

bool raymarch(vec3 origin, vec3 target, float stride, float maxSteps, out vec3 hit) {

    // Initialize to off screen
    hit = vec3(-1.0);

    // If the line is degenerate, make it cover at least one pixel
    // to avoid handling zero-pixel extent as a special case later
    target.xy += vec2((length(target - origin) < 0.0001) ? 0.01 : 0.0);

    vec3 delta = target - origin;

    // Permute so that the primary iteration is in x to reduce
    // large branches later
    bool permute = false;
    if (abs(delta.x) < abs(delta.y)) {
        // More-vertical line. Create a permutation that swaps x and y in the output
        permute = true;

        // Directly swizzle the inputs
        delta = delta.yxz;
        target = target.yxz;
        origin = origin.yxz;
    }

    // From now on, "x" is the primary iteration direction and "y" is the secondary one
    float stepDirection = sign(delta.x);
    float invdx = stepDirection / delta.x;
    vec3 dP = vec3(stepDirection, invdx * delta.y, invdx * delta.z);

    // Scale derivatives by the desired pixel stride
    dP *= stride;

    float stepCount = 0;

    // target.x is never modified after this point, so pre-scale it by 
    // the step direction for a signed comparison
    float end = target.x * stepDirection;

    for (vec3 P = origin;
    ((P.x * stepDirection) <= end) &&
    (stepCount < maxSteps);
    P += dP, stepCount += 1) {

        hit = permute ? P.yxz : P;
        if (SampleDepth(toScreenSpace(hit.xy)) < -P.z) return true;
    }

    return false;
}