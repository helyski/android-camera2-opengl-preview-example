package com.tjn.gles.example;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GLProgramUV {

    public static final String TAG = "GLProgramUV";

    // program id
    private int _program;
    // window position
    public final int mWinPosition;
    // texture id
    private int _textureI;
    private int _textureII;
    private int _textureIII;
    // texture index in gles
    private int _tIindex;
    private int _tIIindex;
    private int _tIIIindex;
    // vertices on screen
    private float[] _vertices;
    // handles
    private int _positionHandle = -1, _coordHandle = -1 ;
    private int mMVPMatrixHandle = -1;
    private int _yhandle = -1, _uhandle = -1, _vhandle = -1;
    private int _ytid = -1, _utid = -1, _vtid = -1;
    // vertices buffer
    private ByteBuffer _vertice_buffer;
    private ByteBuffer _coord_buffer;
    // video width and height
    private int _video_width = -1;
    private int _video_height = -1;
    // flow control
    private boolean isProgBuilt = false;
    private final int vertexCount = 8 / 2; //(vert.length / number of coordinates per vertex in this array )
    /**
     * position can only be 0~4:<br/>
     * fullscreen => 0<br/>
     * left-top => 1<br/>
     * right-top => 2<br/>
     * left-bottom => 3<br/>
     * right-bottom => 4
     */
    public GLProgramUV(int position) {
        if (position < 0 || position > 4) {
            throw new RuntimeException("Index can only be 0 to 4");
        }
        mWinPosition = position;
        setup(mWinPosition);
    }

    /**
     * prepared for later use
     */
    public void setup(int position) {
        switch (mWinPosition) {
            case 1:
                _vertices = squareVertices1;
                _textureI = GLES20.GL_TEXTURE0;
                _textureII = GLES20.GL_TEXTURE1;
                _textureIII = GLES20.GL_TEXTURE2;
                _tIindex = 0;
                _tIIindex = 1;
                _tIIIindex = 2;
                break;
            case 2:
                _vertices = squareVertices2;
                _textureI = GLES20.GL_TEXTURE3;
                _textureII = GLES20.GL_TEXTURE4;
                _textureIII = GLES20.GL_TEXTURE5;
                _tIindex = 3;
                _tIIindex = 4;
                _tIIIindex = 5;
                break;
            case 3:
                _vertices = squareVertices3;
                _textureI = GLES20.GL_TEXTURE6;
                _textureII = GLES20.GL_TEXTURE7;
                _textureIII = GLES20.GL_TEXTURE8;
                _tIindex = 6;
                _tIIindex = 7;
                _tIIIindex = 8;
                break;
            case 4:
                _vertices = squareVertices4;
                _textureI = GLES20.GL_TEXTURE9;
                _textureII = GLES20.GL_TEXTURE10;
                _textureIII = GLES20.GL_TEXTURE11;
                _tIindex = 9;
                _tIIindex = 10;
                _tIIIindex = 11;
                break;
            case 0:
            default:
                _vertices = squareVertices;
                _textureI = GLES20.GL_TEXTURE0;
                _textureII = GLES20.GL_TEXTURE1;
                _textureIII = GLES20.GL_TEXTURE2;
                _tIindex = 0;
                _tIIindex = 1;
                _tIIIindex = 2;
                break;
        }
    }

    public boolean isProgramBuilt() {
        return isProgBuilt;
    }

    public void buildProgram() {
        // TODO createBuffers(_vertices, coordVertices);
        //shader必须要被编译然后再添加到一个OpenGL ES program中,然后这个progrem会被用来绘制shape.
        //注意: 编译OpenGL shader和把它们绑定到program上是非常消耗CPU的,所以应该要尽量避免多次执行这些工作.
        if (_program <= 0) {
            _program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);//VERTEX_SHADER, FRAGMENT_SHADER: 这些代码必须在使用之前编译,
        }
        Log.i(TAG, "_program = " + _program);

        /*
         * get handle for "vPosition" and "a_texCoord"
         */
        /*开始绘制shape*/

        //获取顶点shader的vPosition成员的句柄
        _positionHandle = GLES20.glGetAttribLocation(_program, "vPosition");
        Log.i(TAG, "_positionHandle = " + _positionHandle);
        checkGlError("glGetAttribLocation vPosition");
        if (_positionHandle == -1) {
            throw new RuntimeException("Could not get attribute location for vPosition");
        }
        //获取顶点shader的a_texCoord成员的句柄
        _coordHandle = GLES20.glGetAttribLocation(_program, "a_texCoord");
        Log.i(TAG, "_coordHandle = " + _coordHandle);
        checkGlError("glGetAttribLocation a_texCoord");
        if (_coordHandle == -1) {
            throw new RuntimeException("Could not get attribute location for a_texCoord");
        }

        /*
         * get uniform location for y/u/v, we pass data through these uniforms
         */
        _yhandle = GLES20.glGetUniformLocation(_program, "tex_y");
        Log.i(TAG, "_yhandle = " + _yhandle);
        checkGlError("glGetUniformLocation tex_y");
        if (_yhandle == -1) {
            throw new RuntimeException("Could not get uniform location for tex_y");
        }
        _uhandle = GLES20.glGetUniformLocation(_program, "tex_uv");
        Log.i(TAG, "_uhandle = " + _uhandle);
        checkGlError("glGetUniformLocation tex_u");
        if (_uhandle == -1) {
            throw new RuntimeException("Could not get uniform location for tex_uv");
        }
        isProgBuilt = true;
    }

    /**
     * build a set of textures, one for R, one for G, and one for B.
     */
    public void buildTextures(Buffer y, Buffer uv, int width, int height) {
        boolean videoSizeChanged = (width != _video_width || height != _video_height);
        if (videoSizeChanged) {
            _video_width = width;
            _video_height = height;
            Log.i(TAG, "buildTextures videoSizeChanged: w=" + _video_width + " h=" + _video_height);
        }

        // building texture for Y data
        if (_ytid < 0 || videoSizeChanged) {
            if (_ytid >= 0) {
                Log.i(TAG, "glDeleteTextures Y");
                GLES20.glDeleteTextures(1, new int[]{_ytid}, 0);
                checkGlError("glDeleteTextures");
            }
            // GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            checkGlError("glGenTextures");
            _ytid = textures[0];
            Log.i(TAG, "glGenTextures Y = " + _ytid);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _ytid);
        checkGlError("glBindTexture");
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, _video_width, _video_height, 0,
                GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, y);
        checkGlError("glTexImage2D");
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // building texture for U data
        if (_utid < 0 || videoSizeChanged) {
            if (_utid >= 0) {
                Log.i(TAG, "glDeleteTextures U");
                GLES20.glDeleteTextures(1, new int[]{_utid}, 0);
                checkGlError("glDeleteTextures");
            }
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            checkGlError("glGenTextures");
            _utid = textures[0];
            Log.i(TAG, "glGenTextures U = " + _utid);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _utid);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, _video_width >> 1, _video_height >> 1, 0,
                GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uv);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    /**
     * render the frame
     * the YUV data will be converted to RGB by shader.
     */
    public void drawFrame(float[] mvpMatrix) {
        //Add program to OpenGL ES environment
        GLES20.glUseProgram(_program);
        checkGlError("glUseProgram");
        //准备_positionHandle坐标数据
        GLES20.glVertexAttribPointer(_positionHandle, 2, GLES20.GL_FLOAT, false, 8, _vertice_buffer);
        checkGlError("glVertexAttribPointer mPositionHandle");
        //启用_positionHandle的句柄
        GLES20.glEnableVertexAttribArray(_positionHandle);

        //准备_coordHandle坐标数据
        GLES20.glVertexAttribPointer(_coordHandle, 2, GLES20.GL_FLOAT, false, 8, _coord_buffer);
        checkGlError("glVertexAttribPointer maTextureHandle");
        //启用_coordHandle的句柄
        GLES20.glEnableVertexAttribArray(_coordHandle);

        // bind textures
        GLES20.glActiveTexture(_textureI);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _ytid);
        GLES20.glUniform1i(_yhandle, _tIindex);

        GLES20.glActiveTexture(_textureII);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _utid);
        GLES20.glUniform1i(_uhandle, _tIIindex);

//        GLES20.glActiveTexture(_textureIII);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _vtid);
//        GLES20.glUniform1i(_vhandle, _tIIIindex);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount); //选择绘图方式-三角形绘图
        GLES20.glFinish();

        GLES20.glDisableVertexAttribArray(_positionHandle);
        GLES20.glDisableVertexAttribArray(_coordHandle);

        // get handle to shape's transformation matrix
        //得到旋转矩阵的句柄
        mMVPMatrixHandle = GLES20.glGetUniformLocation(_program, "uMVPMatrix");

        // Pass the projection and view transformation to the shader
        //把投影和视图传递给着色器
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the triangle
        //画出该形状
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 4);

        // Disable vertex array
        //禁用顶点数组
        GLES20.glDisableVertexAttribArray(_positionHandle);
    }

    /**
     * create program and load shaders, fragment shader is very important.
     */
    public int createProgram(String vertexSource, String fragmentSource) {
        // create shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        // just check
        Log.i(TAG, "vertexShader = " + vertexShader);
        Log.i(TAG, "pixelShader = " + pixelShader);

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ", null);
                Log.e(TAG, GLES20.glGetProgramInfoLog(program), null);
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    /**
     * create shader with given source.
     */
    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":", null);
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    /**
     * these two buffers are used for holding vertices, screen vertices and texture vertices.
     */
    void createBuffers(float[] vert) {  //准备好需要绘制的顶点Buffer数据
        _vertice_buffer = ByteBuffer.allocateDirect(vert.length * 4);//initialize vertex byte buffer for shape coordinates
        _vertice_buffer.order(ByteOrder.nativeOrder());       // use the device hardware's native byte order
        _vertice_buffer.asFloatBuffer().put(vert);// create a floating point buffer from the ByteBuffer. and add the coordinates to the FloatBuffer
        _vertice_buffer.position(0); //set the buffer to read the first coordinate
        Log.e(TAG , "jeff, vert = (" + vert[0] + ", " + vert[1] + "), (" + vert[2] + ", " + vert[3] + "), (" + vert[4] + ", " + vert[5] + "), (" + vert[6] + ", " + vert[7] + ")");

        if (_coord_buffer == null) {
            _coord_buffer = ByteBuffer.allocateDirect(coordVertices.length * 4);
            _coord_buffer.order(ByteOrder.nativeOrder());
            _coord_buffer.asFloatBuffer().put(coordVertices);
            _coord_buffer.position(0);
        }
        Log.e(TAG , "jeff, cord = (" + coordVertices[0] + ", " + coordVertices[1] + "), (" + coordVertices[2] + ", " + coordVertices[3] + "), (" + coordVertices[4] + ", " + coordVertices[5] + "), (" + coordVertices[6] + ", " + coordVertices[7] + ")");
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "***** " + op + ": glError " + error, null);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    //static float[] squareVertices = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,}; // fullscreen
    //默认情况下,OpenGL ES设定GLSurfaceView的frame的中心为坐标的原点[0,0,0](X,Y,Z),frame的右上角的坐标为[1,1,0],左下角的坐标为[-1,-1,0]
    //图形坐标是按逆时针来排序的
    //顶点方向设置
    //static float[] squareVertices = {1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,}; // fullscreen rotate 90
    static float[] squareVertices = {-1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f,}; // test  //控件
    static float[] squareVertices1 = {-1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f,}; // left-top

    static float[] squareVertices2 = {0.0f, -1.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f,}; // right-bottom

    static float[] squareVertices3 = {-1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,}; // left-bottom

    static float[] squareVertices4 = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,}; // right-top

    //     private static float[] coordVertices = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,};// whole-texture
//    private static float[] coordVertices = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,};// whole-texture rotate 90
//    private static float[] coordVertices = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,};// just left right mirro
    private static float[] coordVertices = {1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f,};// 4.24 test success  图/纹理

    //渲染shape顶点的OpenGl ES代码.
    private static final String VERTEX_SHADER = "attribute vec4 vPosition;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "attribute vec2 a_texCoord;\n" +
            "varying vec2 tc;\n" +
            "void main() {\n" +
            "gl_Position = uMVPMatrix * vPosition;\n" +
            "tc = a_texCoord;\n" +
            "}\n";
    //渲染shape的face的颜色和texture的OpenGl ES代码.
    private static final String FRAGMENT_SHADER = "precision mediump float;\n" + "uniform sampler2D tex_y;\n"
            + "uniform sampler2D tex_uv;\n" + "varying vec2 tc;\n" + "const float PI = 3.14159265;\n"
            + "const mat3 convertMat = mat3( 1.0, 1.0, 1.0, 0.0, -0.39465, 2.03211, 1.13983, -0.58060, 0.0 );\n"
            + "void main() {\n"
            + "vec3 yuv;\n"
            + "yuv.x = texture2D(tex_y, tc).r;\n"
            + "yuv.z = texture2D(tex_uv, tc).r - 0.5;\n"
            + "yuv.y = texture2D(tex_uv, tc).a - 0.5;\n"
            + "vec3 color = convertMat * yuv;\n"
            + "vec4 mainColor = vec4(color, 1.0);\n"
            + "gl_FragColor = mainColor;\n" + "}\n";

}
