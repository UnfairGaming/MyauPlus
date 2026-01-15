#version 120

uniform vec2 rectSize;
uniform float radius;
uniform float softness;
uniform vec4 color;

float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {
    return length(max(abs(centerPos) - size, 0.0)) - radius;
}

void main() {
    vec2 totalSize = rectSize + softness * 2.0;

    vec2 pos = gl_TexCoord[0].st * totalSize;

    vec2 center = totalSize / 2.0;
    vec2 p = pos - center;

    float dist = roundedBoxSDF(p, rectSize / 2.0 - radius, radius);

    float alpha = 1.0 - smoothstep(0.0, softness, dist);

    gl_FragColor = vec4(color.rgb, color.a * alpha);
}