#version 120

uniform vec2 location;
uniform vec2 rectSize;
uniform vec4 color;
uniform float radius;
uniform float softness;

float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {
    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;
}

void main() {

    vec2 center = location + rectSize / 2.0;
    vec2 pos = gl_FragCoord.xy - center;
    
    float dist = roundedBoxSDF(pos, rectSize / 2.0, radius);
    
    float smoothedAlpha = 1.0 - smoothstep(0.0, softness, dist);
    
    if (smoothedAlpha <= 0.0) discard;

    gl_FragColor = vec4(color.rgb, color.a * smoothedAlpha);
}