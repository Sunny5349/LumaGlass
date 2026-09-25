#version 150
uniform sampler2D Source;
uniform vec2 Direction;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec3 color = texture(Source, texCoord).rgb * 0.227027;
    color += texture(Source, texCoord + Direction * 0.384615).rgb * 0.316216;
    color += texture(Source, texCoord - Direction * 0.384615).rgb * 0.316216;
    color += texture(Source, texCoord + Direction).rgb * 0.0702705;
    color += texture(Source, texCoord - Direction).rgb * 0.0702705;
    fragColor = vec4(color, 1.0);
}
