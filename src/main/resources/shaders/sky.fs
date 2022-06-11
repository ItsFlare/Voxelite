#version 410 core

out vec4 FragColor;

uniform vec3 sunPos;
uniform vec3 color;
uniform vec2 viewPortResolution;
uniform vec3 direction;
uniform float dayPercentage;
uniform float fov;
uniform mat3 rotation;

void main()
{
    vec3 white = vec3(1,1,1);
    vec3 yellow = vec3(1.0,1.0, 0.0);
    vec3 orange = vec3(1.0,0.5,0.0);
    vec3 red = vec3(1.0,0.0, 0.0);
    vec3 black = vec3(0,0,0);
    vec3 redSky = vec3(0.8,0.8,0.6);
    vec3 sunColor = vec3(0,0,0);
    float PI = 3.14159265358;

    //normalize fragment coordinates
    vec2 viewPortCoord = gl_FragCoord.xy / viewPortResolution.xy;
    vec2 pixelCoord = -1 + 2 * viewPortCoord;
    pixelCoord.x *= viewPortResolution.x / viewPortResolution.y;
    vec3 col;

    vec3 rayDirection= vec3(pixelCoord.x * tan(fov / 2 * PI / 180), pixelCoord.y * tan(fov / 2 * PI / 180), -1);
    rayDirection = rotation * rayDirection;
    rayDirection = normalize(rayDirection);


    float sundot = clamp(dot(rayDirection, sunPos), 0 ,1);

    col =  color;

    sunColor += 0.1*vec3(0.1, 0.3, 0.9)*pow(sundot, 0.7);
    sunColor += 0.3*vec3(1., 0.7, 0.7)*pow(sundot, 1.);
    sunColor += 0.7*vec3(1.)*(pow(sundot, 128) + 0.35);


    float num = clamp(-2 * pow(direction.y,3), -1.,0.);

    // scales the color when camera moves up/down
    col = mix(white, col, -num);


    // responsible for changing sky color when day progresses
    col = mix(orange, col, -1 + 2 * dayPercentage);
    col = mix(red, col, -1 + 2 * dayPercentage);
    col = mix(col + sunColor, color , 0.3);

    col = mix(col, color, (pixelCoord.y + 1) /2 );
    col = mix(black, col, dayPercentage);

    FragColor= vec4(col,1.0);
}