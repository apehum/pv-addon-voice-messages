//? if >=26.3 {
//#version 330
//#extension GL_ARB_separate_shader_objects : require
//
//layout(std140) uniform WaveformParams {
//    vec4 RadiiA;
//    vec4 RadiiB;
//    vec4 Scalars;
//    vec4 Rect;
//};
//
//layout(location = 0) in vec2 texCoord0;
//layout(location = 1) in vec4 vertexColor;
//
//layout(location = 0) out vec4 fragColor;
//?} else if >=1.21.6 {
//#version 150
//
//layout(std140) uniform WaveformParams {
//    vec4 RadiiA;
//    vec4 RadiiB;
//    vec4 Scalars;
//    vec4 Rect;
//};
//
//in vec2 texCoord0;
//in vec4 vertexColor;
//
//out vec4 fragColor;
//?} else {
#version 150

uniform vec4 RadiiA;
uniform vec4 RadiiB;
uniform vec4 Scalars;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;
//?}

const float TAU = 6.28318530718;
const int SEGMENTS = 8;

// how much of its own radius the button gains at full volume
const float CIRCLE_GROWTH = 0.25;
// how far out the outline sits in silence
const float IDLE_REACH = 0.25;

float radiusAt(int segment) {
    int index = segment % SEGMENTS;
    vec4 radii = index < 4 ? RadiiA : RadiiB;
    return radii[index % 4];
}

void main() {
    float amplitude = Scalars.x;
    float outerSize = Scalars.y;
    float circleSize = Scalars.z;
    float blobAlpha = Scalars.w;

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
