#version 410 core

out vec4 FragColor;

uniform vec3 sunPos;
uniform vec2 viewPortResolution;
uniform float dayPercentage;
uniform float fov;
uniform mat3 rotation;

void main()
{
    vec3 white = vec3(1, 1, 1);
    vec3 yellow = vec3(1.0, 1.0, 0.0);
    vec3 orange = vec3(1.0, 0.5, 0.0);
    vec3 red = vec3(1.0, 0.0, 0.0);
    vec3 black = vec3(0, 0, 0);
    vec3 redSky = vec3(0.8, 0.8, 0.6);
    vec3 blueSky = vec3(0.3, 0.55, 0.8);
    float PI = 3.14159265358;

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

    //direction to horizon vs sun at horizon
    float sunhordot = clamp(dot(vec3(rayDirection.x, 0, rayDirection.z), normalize(vec3(sunPos.x, 0, sunPos.z))), 0, 1);
    float sunhordeg = degrees(acos(sunhordot));

    //direction vs horizon
    float hordot = dot(rayDirection, normalize(vec3(rayDirection.x, 0, rayDirection.z)));
    float hordeg = degrees(acos(hordot));

    //sun vs horizon
    float sunrisedot = dot(sunPos, normalize(vec3(sunPos.x, 0, sunPos.z)));
    float sunrisedeg = degrees(acos(sunrisedot));

    //sun vs up vector
    float daydot = dot(sunPos, vec3(0, 1, 0));
    float daydotNormalized = 0.5 + daydot * 0.5;

    vec3 a = vec3(0.82, 0.57, 0.02); //yellow-orange
    vec3 b = vec3(0.82, 0.37, 0.02); //orange-red
    vec3 c = red;
    vec3 d = vec3(0.60, 0.04, 0.40); //purple

    float range = 30; //range around horizon in deg
    float deg = 90 - degrees(acos(daydot)) + range; //angle of sun relative to horizon in deg
    float interval = (range * 2) / 3; //angle of one interval in deg

    //select appropriate range and interpolate
    vec3 sunsetColor = vec3(0);
    if(deg > 0 && deg <= interval) sunsetColor = mix(d, c, deg / interval);
    if(deg > interval && deg <= interval * 2) sunsetColor = mix(c, b, (deg - interval) / interval);
    if(deg > interval * 2 && deg <= interval * 3) sunsetColor = mix(b, a, (deg - interval * 2) / interval);

    float horizonFactor = pow(clamp(1 - hordeg / 45, 0, 0.9), 2);
    float verticalIntensity = clamp(1 - sunrisedeg / 45, 0.5, 0.75);

    //angle relative to sun at horizon where horizon is visible
    //360 if sun overhead, 240 if sund at horizon
    float horizonCoverageAngle = clamp(240 * 2 * daydotNormalized, 0, 360);

    //calculate horizon intensity based on coverage angle
    //full intensity at daytime, highlight at sunset
    float horizontalIntensity = max(clamp(1 - sunhordeg / horizonCoverageAngle, 0, 1), daydot);

    vec3 skyColor = mix(black, blueSky, dayPercentage);
    vec3 horizonColor = mix(daydot > 0 ? white : black, sunsetColor, clamp(1 - sunrisedeg / range, 0, 1));
    skyColor = mix(skyColor, horizonColor, horizonFactor * verticalIntensity * horizontalIntensity);

    //sun glow
    vec3 sunGlow = vec3(0);
    sunGlow += 0.25 * mix(horizonColor, white, 0.2) * pow(sundot, 32);
    sunGlow += 0.25 * mix(horizonColor, white, 0.2) * pow(sundot, 128);
    skyColor += sunGlow;

    skyColor = mix(black, skyColor, dayPercentage); //darken at night

    FragColor= vec4(skyColor, 1.0);
}