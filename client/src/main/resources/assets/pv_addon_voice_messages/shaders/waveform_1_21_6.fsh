#version 150

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

const float TAU = 6.28318530718;
const int SEGMENTS = 8;

// how much of its own radius the button gains at full volume
const float CIRCLE_GROWTH = 0.25;
// how far out the outline sits in silence
const float IDLE_REACH = 0.25;

vec4 parameterAt(int index) {
    return texelFetch(Sampler0, ivec2(index, 0), 0);
}

float radiusAt(int segment) {
    return parameterAt(segment % SEGMENTS).r;
}

void main() {
    vec4 scalars = parameterAt(SEGMENTS);

    float amplitude = scalars.r;
    float outerSize = floor(scalars.g * 255.0 + 0.5);
    float circleSize = floor(scalars.b * 255.0 + 0.5);
    float blobAlpha = scalars.a;

    float pixelSize = 1.0 / outerSize;
    float circleRadius = circleSize * 0.5 / outerSize;

    vec2 uv = (floor(texCoord0 / pixelSize) + 0.5) * pixelSize;

    vec2 offset = uv - vec2(0.5);
    float dist = length(offset);
    float angle = atan(offset.y, offset.x);

    float circle = circleRadius * (1.0 + amplitude * CIRCLE_GROWTH);
    float room = 0.5 - circle;

    float position = (angle / TAU + 0.5) * float(SEGMENTS);
    int segment = int(floor(position));
    float shape = mix(
        radiusAt(segment),
        radiusAt(segment + 1),
        smoothstep(0.0, 1.0, position - float(segment))
    );

    float reach = IDLE_REACH + (1.0 - IDLE_REACH) * amplitude;
    float blob = circle + room * reach * shape;

    if (dist <= circle) {
        fragColor = vertexColor;
        return;
    }

    if (dist > blob) {
        discard;
    }

    float falloff = smoothstep(0.0, 1.0, 1.0 - (dist - circle) / max(blob - circle, 1e-4));

    fragColor = vec4(vertexColor.rgb, vertexColor.a * blobAlpha * falloff);
}
