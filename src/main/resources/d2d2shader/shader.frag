#version 110

uniform sampler2D texA;

varying vec2 texCoord;

void main() {
    gl_FragColor = texture2D(texA, texCoord);
}