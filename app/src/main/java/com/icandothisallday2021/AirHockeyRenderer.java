package com.icandothisallday2021;

import android.content.Context;
import android.opengl.GLES20;
import static android.opengl.GLES20.*;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AirHockeyRenderer implements GLSurfaceView.Renderer {
    private final Context context;

    private static final int POSITION_COMPONENT_COUNT = 2;
    //Each vertex would need 2 floating point numbers-(x,y)- in 2-dimensional object

    private static final int COLOR_COMPONENT_COUNT = 3;

    private static final int BYTES_PER_FLOAT = 4;//a float in Java has 32bit of precision== 4bytes(there are 4bytes in every float)
    private FloatBuffer vertexData;//will be used to store data in native memory
    //After the final colors are generated, OpenGL will write them into a block of memory known as the frame buffer,
    //and Android will then display this frame buffer on the screen.

    private static final int STRIDE = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;
    //STRIDE : (the interval(term) between each position or each color)
    //==how many bytes are between each position so that OpenGL knows how far it has to skip.
    //float[] itemVerticesAndColor : have both a position and a color attribute in the same data array,
    //Then Open GL can no longer assume(예측하다) that the next position follows.
    //It will have to skip over the color for the current vertex if it wants to read the position for the next vertex.

    private int program;//the ID of the linked program.

    private static final String A_COLOR = "a_Color";
    private int aColorLocation;
    //A uniform’s location is unique to a program object:
    // even if we had the same uniform name in two different programs, that doesn’t mean that they’ll share the same location.

    private static final String A_POSITION = "a_Position";
    private int aPositionLocation;

    private final float[] projectionMatrix = new float[16];
    private int uMatrixLocation;// to hold the location of the matrix uniform

    public AirHockeyRenderer(Context context) {
        this.context = context;
        float[] itemVerticesAndColors = { //AirHockey ground(rectangular table)
                //Everything we want to build needs to be defined in terms of point,line, and triangles.
                //rectangle is two triangles joined together. So use this---

                //Order of coordinates : X, Y, R, G, B
                //Triangle Fan
                0,  0,  1f, 1f, 1f,
                -0.5f,  -0.8f,  0.7f, 0.7f, 0.7f,
                0.5f,   -0.8f,  0.7f, 0.7f, 0.7f,
                0.5f,   0.8f,   0.7f, 0.7f, 0.7f,
                -0.5f,   0.8f,  0.7f, 0.7f, 0.7f,
                -0.5f,   -0.8f, 0.7f, 0.7f, 0.7f,

                //Center Line 1
                -0.5f, 0f,  1f, 0f, 0f,
                0.5f, 0f,   0f, .5f, .6f,

                //Mallet 1
                0f, -0.4f,   1f, 0f, 0f,
                //Mallet 2
                0f, 0.4f,    0f, 0f, 1f
        };//Whenever we want to represent(표현하다) an object in OpenGL, we need to think about
        //<<how we can compose(구성하다) it in terms(관점) of points, lines, and triangles.>>

        vertexData = ByteBuffer.allocateDirect(itemVerticesAndColors.length * BYTES_PER_FLOAT)
                //allocated a block of native memory
                //->this memory will not be managed by the garbage collector.
                //(parameter) : how large the block of memory should be (in byte).
                //-->vertices are stored in an array of float and there are 4 bytes per float
                .order(ByteOrder.nativeOrder())
                //The bytes can be ordered either from most significant to least significant or from least to most.
                //--It is important that we use the same order as the platform.
                .asFloatBuffer();
                //We'd rather not deal with individual bytes directly.
                //so we call this method to get a FloatBuffer that reflects(반영하) the underlying(기본) bytes.

        vertexData.put(itemVerticesAndColors);//Copy data from Dalvik's memory to native memory
        //-The memory will be freed when the process gets destroyed
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f,0.0f,0.0f,0.0f);//set the background clear color to red.
        String vertexShaderCode =   "uniform mat4 u_Matrix;\n" +//mat4 -> this uniform will represent a 4 x 4 matrix.
                                    "attribute vec4 a_Position;\n" +
                                    "attribute vec4 a_Color;\n" +
                                    "varying vec4 v_Color;\n" +
                                    "void main() {\n" +
                                    "    v_Color = a_Color;\n" +
                                    "    gl_Position = u_Matrix * a_Position;\n" +
                //our vertex array is interpreted as existing in a virtual coordinate space, as defined by the matrix.
                //The matrix will transform the coordinates from this virtual coordinate space back into normalized device coordinates.
                                    "gl_PointSize = 10.0; }\n";
        String fragmentShaderCode = "precision mediump float; " +
                                    "varying vec4 v_Color;" +
                                    "void main() {" +
                                    "gl_FragColor = v_Color;" +
                                    "}";


        int vertexShader = ShaderHelper.compileShader(GL_VERTEX_SHADER,vertexShaderCode);
        int fragmentShader = ShaderHelper.compileShader(GL_FRAGMENT_SHADER,fragmentShaderCode);

        program = ShaderHelper.linkProgram(vertexShader,fragmentShader);

        uMatrixLocation = glGetUniformLocation(program, "u_Matrix");

        if(LoggerConfig.ON) {
            ShaderHelper.validateProgram(program);
        }

        GLES20.glUseProgram(program);

        aPositionLocation = GLES20.glGetAttribLocation(program, A_POSITION);//where to find the data for this attribute.
        aColorLocation = GLES20.glGetAttribLocation(program,A_COLOR);

        //To start reading at the very beginning
        vertexData.position(0);//when OpenGL reads from our buffer, it will start reading at this position(at the very beginning).
        //to tell OpenGL, where to read the data for the attribute a_Position.
        //OpenGL finds the data for a_Position in the buffer vertexData(native buffer for store the positions)
 

        //To associate the color data with a_Color in the shaders.
        GLES20.glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GLES20.GL_FLOAT, false, STRIDE, vertexData);
        //⭐️ When we pass in our attribute data, we need to make sure to also pass int he right values for the component count and the stride.
        //< parameter >
        //1. int index : attribute location
        //2. int size : data count per attribute, or how many components are associated with each vertex for this attribute.
        //3. int type :  type of data
        //4. boolean normalized : only applies if we use integer data---> ignore now
        //5. int stride : applies when we store more than one attribute in a single array
        //                how many bytes it needs to skip to read the color for the next vertex
        //6. Buffer ptr :  for OpenGL where to read the data
        //   (It will start reading from the buffer's current position->
        //    it would probably try to read past the end of the buffer->crash our App, so we call vertexData.position(0))

        //To linked our data to the attribute, we need to enable the attribute.
        GLES20.glEnableVertexAttribArray(aPositionLocation);//to tell OpenGL, where to find all the data it needs.


        //Bind our data, specified by the variable vertexData, to the vertex attribute at the location A_COLOR LOCATION.
        vertexData.position(POSITION_COMPONENT_COUNT);
        //when OpenGL reads from our buffer, it will start reading at this(the first color attribute not the first position attribute).
        //parameter : to skip over the first position component size into account / set to the position of the very first color attribute.
        GLES20.glVertexAttribPointer(aColorLocation,COLOR_COMPONENT_COUNT,GL_FLOAT, false, STRIDE, vertexData);
        GLES20.glEnableVertexAttribArray(aColorLocation);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //set the OpenGL viewport to fill the entire surface.
        GLES20.glViewport(0,0,width,height);

        //Defining an Orthographic Projection
        final float aspectRatio = width>height ? (float)width/(float)height : (float)height/(float)width; //aspect ratio : 가로 x 세로 영상비

        //Landscape
        if(width>height) Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);
        //Portrait or square
        else Matrix.orthoM(projectionMatrix, 0 , -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
        // orthoM() <parameter>
        //1. float[] m : The destination(도착점) array-it store the orthographic projection matrix
        //2. int mOffset : the offset into m into which the result is written
        //3. float left : the minimum range of the x-axis   //4. float right : the maximum range of the x-axis
        //5. float bottom : the minimum range of the y-axis //6. float top : the maximum range of the y-axis
        //7. float near : the minimum range of the z-axis   //8. float far : the maximum range of the z-axis
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);//clear the rendering surface

        //Sending the matrix to the shader (assign the matrix)
        glUniformMatrix4fv(uMatrixLocation,1,false,projectionMatrix,0);

        //Drawing the Table
        /*GLES20.glUniform4f(aColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);//update the value of u_Color in our shader code
        glUniform4f()---X--->Since we've already associated our vertex data with a_Color, we no longer need this code*/
        //uniforms don't have default components, so we need to provide all four components for defined the uniform as a vec4 in our shader.
        GLES20.glDrawArrays(GL_TRIANGLE_FAN, 0, 6);
        //< parameter >
        //To draw triangles, we need to pass in at least three vertices per triangle.
        //0 : OpenGL to read in vertices starting at the beginning of our vertex array.
        //6 : OpenGL to read int six vertices.

        //Drawing the Dividing Line
        //GLES20.glUniform4f(aColorLocation, 1.0f, 0.0f ,0.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 6, 2);
        //6 : We start six vertices after the first vertex
        //2 : Since there are two vertices per line

        //Drawing the Mallets as Points
        //GLES20.glUniform4f(aColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 8, 1);//Draw the first mallet blue.
        //Draw the second mallet red.
        //GLES20.glUniform4f(aColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 9, 1);
    }
}
//OpenGL pipeline
//Read Vertex Data->Execute Vertex Shader->Assemble Primitives->Rasterize Primitives->Execute Fragment Shader->Write to Frame Buffer->See it on the Screen
//Shaders tell the GUP(graphics processing unit) how to draw our data.
//1. Vertex Shader : generates the final position of each vertex and is run once per vertex.
//-->OpenGL will take /the visible set of vertices/ and assemble them(final position) into points, lines, and triangles.
//2. Fragment Shader : generates the final color of each fragment of a point, line, or triangle and is run once per fragment
//+) fragment : a small area of a single color on a computer screen (analogous to a pixel)
//We need to define both of them before we cen draw anything to the screen.

