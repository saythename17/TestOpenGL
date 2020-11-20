precision mediump float;
//자료형 정의와 비슷한 개념 lowp, mediump, highp (~precision)
//defines the default precision(how much use the memory byte) for all floating point data types in the fragment shader.
//in vertex shader-- accuracy(정확도) is more important(vertex's position)-->highp : default type

uniform vec4 u_Color;
//uniform : keeps the same value for all vertices until we change it again.
//When OprnGL liknks our shders into a program, it will associate each uniform defined int he vertex shader with a locaiton number.
//We'll use this uniform to set the color of what we're drawing.
//u_Color : 4-component vector(RGBA)

void main() {
    gl_FragColor = u_Color;
}

