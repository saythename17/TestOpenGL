attribute vec4 a_position;

void main() {
    gl_Position = a_Position;
}

//This vertex shader will be called once for evey single vertex that we've defined.
//It will receive the current vertex's position in the a_Position attribute, which is defined to be a vec4.
//OpenGL will use the value stored in gl_Position as the final position for the current vertex
//and start assembling vertices into points, lines, and triangles.

//vec4 : A vector consisting of( : ~로 구성되다 ) four components. (x,y,z,w)
//x,y,z : 3D position \ w : special coordinate
//attribute keyword : how we feed these attributes(such as a color and a position,in vertex) into our shader