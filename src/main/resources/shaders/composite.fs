#version 330

in vec2 pixel;

out vec4 FragColor;

uniform vec2 planes;
uniform sampler2D opaque;
uniform sampler2D transparent;
uniform sampler2D normal;
uniform sampler2D mer;
uniform sampler2D depth;
uniform mat4 projection;

float LinearDepth(float depth) {
    float z_n = 2 * depth - 1;
    float near = planes.x;
    float far = planes.y;

    return 2 * near * far / (far + near - z_n * (far - near));
}

float SampleDepth(vec2 uv) {
    return LinearDepth(texture(depth, uv).x);
}

vec3 toViewSpace(vec2 screen, float z) {
    vec2 viewport = textureSize(opaque, 0);
    float aspectRatio = viewport.x / viewport.y;
    vec2 clipCoord = (2 * screen) - 1;
    clipCoord.x *= aspectRatio;
    float fov = 120;

    vec3 rayDirection = vec3(clipCoord * tan(radians(fov / 2)), -1);
    rayDirection = normalize(rayDirection);

    return rayDirection * z;
}

vec3 toViewSpace(vec2 screen) {
    return toViewSpace(screen, SampleDepth(screen));
}

vec3 toScreenSpace(vec3 view) {
    vec4 projectedCoords = projection * vec4(view, 1);
    projectedCoords.xy /= projectedCoords.w;
    projectedCoords.xy = projectedCoords.xy * 0.5 + 0.5;
    return projectedCoords.xyz;
}

const float MAX_DISTANCE = 50;
const float RESOLUTION  = 0.3;
const float EPSILON   = 0.5;
const int SEARCH_STEPS = 30;
const int MAX_STEPS = 10;
const int RAY_STRIDE = 5;

//Broken
vec3 rayMarchScreen(vec3 position, vec3 direction) {
    vec2 texSize  = textureSize(opaque, 0).xy;

    vec3 originV = position;
    vec3 targetV   = position + (direction * MAX_DISTANCE);

    vec3 originF    = toScreenSpace(originV);
    originF.xy *= texSize;
    vec3 targetF      = toScreenSpace(targetV);
    targetF.xy *= texSize;

    float deltaX    = targetF.x - originF.x;
    float deltaY    = targetF.y - originF.y;
    float useX      = abs(deltaX) >= abs(deltaY) ? 1 : 0;
    float delta     = mix(abs(deltaY), abs(deltaX), useX) * clamp(RESOLUTION, 0.0, 1.0);
    vec2  increment = vec2(deltaX, deltaY) / max(delta, 0.001);


    vec2 frag  = originF.xy;
    vec2 uv = originF.xy / texSize;

    for (float i = 0; i < min(int(delta), 1000); i++) {
        frag += increment;
        uv = frag / texSize;
        if(uv.x < 0 || uv.x > 1) break;
        if(uv.y < 0 || uv.y > 1) break;

        float search = mix((frag.y - originF.y) / deltaY, (frag.x - originF.x) / deltaX, useX);

        float viewDistance = (originV.z * targetV.z) / mix(targetV.z, originV.z, search);
        float depth        = viewDistance - SampleDepth(uv);

        if (depth > 0 && depth < EPSILON) {
            return(vec3(uv, 1));
        }
    }

    return vec3(0);
}
vec3 search(vec3 position, vec3 direction) {
    float z;

    for (int i = 0; i < SEARCH_STEPS; ++i){
        direction *= 0.8;

        z = SampleDepth(toScreenSpace(position).xy);

        position += sign(z - position.z) * direction;
    }

    return vec3(toScreenSpace(position).xy, z);
}
vec3 rayMarchView(vec3 position, vec3 direction) {
    direction *= RAY_STRIDE;

    for(int i = 0; i < MAX_STEPS; i++) {
        position += direction;
        vec2 ss = toScreenSpace(position).xy;
        if(ss.y < 0 || ss.y > 1) break;
        if(ss.x < 0 || ss.x > 1) break;

        float z = SampleDepth(ss);

        if(position.z < z) {
            return search(position, direction);
        }
    }

    return vec3(0);
}

vec4 blend(vec2 uv) {
    vec4 o = texture(opaque, uv);
    vec4 t = texture(transparent, uv);
    return mix(o, t, t.a);
}

//Hermanns. Screen Space Cone Tracing for Glossy Reflections. 2015.
float CalculateFade(vec2 hit) {
    float I_end = 1;
    float I_start = 0.01;
    float D_boundary = length(hit.xy - vec2(0.5)) * 2;
    float f_border = clamp((D_boundary - I_start) / (I_end - I_start), 0, 1);

    return f_border;
}

void main() {
    vec4 o = texture(opaque, pixel);
    float opaqueZ = LinearDepth(o.a);
    o.a = 1;

    vec4 t = texture(transparent, pixel);
    vec3 n = texture(normal, pixel).xyz;
    if (n == 0) return;
    n = 2 * n - 1;
    vec3 mer = texture(mer, pixel).rgb;

    float roughness = mer.b;

    vec3 viewPos = toViewSpace(pixel, opaqueZ);
    vec3 rayDirection = normalize(viewPos);
    rayDirection = reflect(rayDirection, n);

    if(dot(vec3(0, 0, -1), rayDirection) > 0) {
        vec3 hit = rayMarchView(viewPos, rayDirection);

        if(hit.z != 0) o = mix(mix(blend(hit.xy), o, roughness), o, CalculateFade(hit.xy));
    }


    FragColor = mix(o, t, t.a);
    //FragColor = mix(vec4(rayDirection, 1), FragColor, 0.00001);
    //FragColor = mix(vec4(n, 1), FragColor, 0.00001);
}