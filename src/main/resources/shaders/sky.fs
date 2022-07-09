#version 410 core

out vec4 FragColor;

uniform vec3 sunPos;
uniform vec3 color;
uniform vec2 viewPortResolution;
uniform float dayPercentage;
uniform float fov;
uniform mat3 rotation;

const float PI = 3.14159265359;
const vec3 white = vec3(1);
const vec3 black = vec3(0);
const vec3 blueSky = vec3(0.3, 0.55, 0.8);
const float horizonAngularSize = 45;
const float horizonAngularOffset = 5;
void main()
{
    //normalize fragment coordinates
    vec2 viewPortCoord = gl_FragCoord.xy / viewPortResolution.xy;
    vec2 clipCoord = -1 + 2 * viewPortCoord;
    clipCoord.x *= viewPortResolution.x / viewPortResolution.y;

    vec3 rayDirection = vec3(clipCoord.xy * tan(radians(fov / 2)), -1);
    rayDirection = rotation * rayDirection;
    rayDirection = normalize(rayDirection);

    //direction vs sun
    float sundot = clamp(dot(rayDirection, sunPos), 0, 1);
    float sundeg = degrees(acos(sundot));
    float moondot = clamp(dot(rayDirection, -sunPos), 0, 1);

    //direction to horizon vs sun at horizon
    float sunhordot = dot(normalize(vec3(rayDirection.x, 0, rayDirection.z)), normalize(vec3(sunPos.x, 0, sunPos.z)));
    float sunhordeg = degrees(acos(sunhordot));

    //sun vs up vector
    float daydot = dot(sunPos, vec3(0, 1, 0)); //in range of [-1,1]
    float daydotNormalized = 0.5 + daydot * 0.5;
    float daydeg = degrees(acos(daydot));

    //sun vs horizon
    float sunrisedeg = 90 - daydeg;

    //direction vs horizon
    float hordeg = 90 - degrees(acos(dot(rayDirection, vec3(0, 1, 0))));

    //base color
    vec3 skyColor = mix(black, blueSky, dayPercentage);

    vec3 a = vec3(0.82, 0.57, 0.02); //yellow-orange
    vec3 b = vec3(0.82, 0.37, 0.02); //orange-red
    vec3 c = vec3(1.00, 0.00, 0.00); //red
    vec3 d = vec3(0.70, 0.00, 0.30); //purple

    float deg = sunrisedeg + horizonAngularSize; //angle of sun relative to horizon in deg with offset for selection
    float interval = (horizonAngularSize * 2) / 3; //angle of one interval in deg

    //select appropriate range and interpolate
    vec3 sunsetColor = vec3(0);
    if(deg > 0 && deg <= interval) sunsetColor = mix(d, c, deg / interval);
    else if(deg > interval && deg <= interval * 2) sunsetColor = mix(c, b, (deg - interval) / interval);
    else if(deg > interval * 2 && deg <= interval * 3) sunsetColor = mix(b, a, (deg - interval * 2) / interval);

    float horizonFactor = pow(clamp(1 - (hordeg + horizonAngularOffset) / horizonAngularSize, 0, 1), 1.5);
    float verticalIntensity = clamp(1 - (sunrisedeg) / horizonAngularSize, 0.5, 1.0);

    //angle relative to sun at horizon where horizon is visible
    float horizonCoverageAngle = clamp(daydotNormalized * 360, 120, 360);

    //calculate horizon intensity based on coverage angle
    //full intensity at daytime, highlight at sunset
    float horizontalIntensity = max(clamp(1 - sunhordeg / horizonCoverageAngle, 0, 1), 1 - daydeg / 90);

    //horizon
    vec3 horizonColor = mix(sunrisedeg > 0 ? white : skyColor, sunsetColor, clamp(1 - abs(sunrisedeg) / horizonAngularSize, 0, 1));
    skyColor = mix(skyColor, horizonColor, horizonFactor * verticalIntensity * horizontalIntensity * 0.8);

    //sun glow
    vec3 sunColor = vec3(1.00, 0.94, 0.72);
    vec3 sunGlow = mix(sunColor, horizonColor, 0.5);
    float sunAlpha = 0.25 * pow(sundot, 64);
    skyColor = mix(skyColor, sunGlow, sunAlpha);

    //moon glow
    vec3 moonColor = white;
    vec3 moonGlow = mix(moonColor, horizonColor, 0.1);
    float moonAlpha = 0.1 * pow(moondot, 16);
    skyColor = mix(skyColor, moonGlow, moonAlpha);

    FragColor = vec4(skyColor, 1.0);
}