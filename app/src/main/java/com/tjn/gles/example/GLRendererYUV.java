package com.tjn.gles.example;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRendererYUV implements GLSurfaceView.Renderer {
    public static final String TAG = "GLFrameRenderer";

    private GLSurfaceView mTargetSurface;
    private GLProgramUV mProg = new GLProgramUV(0);
    private int mScreenWidth, mScreenHeight;
    private int mVideoWidth, mVideoHeight;
    private ByteBuffer mYbuffer;
    private ByteBuffer mUVbuffer;
    private static int angle = -1;
    public GLRendererYUV(GLSurfaceView surface) {
        mTargetSurface = surface;
    }


    public void SetAngle(int Angle) {
        angle = Angle;
        Log.d(TAG, "set angle: " + angle);
    }
    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    //最终变换矩阵
    private final float[] mMVPMatrix = new float[16];
    //投影矩阵
    private final float[] mProjectionMatrix = new float[16];
    //相机矩阵
    private final float[] mViewMatrix = new float[16];
    private float[] mRotationMatrix = new float[16];

    //在OpenGL surface被创建时的回调。
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "GLFrameRenderer :: onSurfaceCreated");
        if (!mProg.isProgramBuilt()) {
            //在绘制之前,需要先将各shape初始化并加载,如果这些shape的坐标不会在执行的时候变化,
            //那么可以在onSurfaceCreated()中进行初始化和加载工作,这样会更省内存和提高处理效率.
            mProg.buildProgram();
            Log.i(TAG, "GLFrameRenderer :: buildProgram done");
        }
    }

    //Surface大小变化时调用，例如横屏转为竖屏、GLSurfaceView大小变化等。
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.e(TAG, "GLFrameRenderer :: onSurfaceChanged width = " + width + " height = " + height);
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method //该投影矩阵会被应用到对象的目标坐标系
        //根据绘制对象在GLSurfaceView中的宽和高的坐标来转换的.如果没有这个计算,图像会变形.
        //Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -1, 1, 3, 7);
    }
    //此方法在渲染一帧图像时调用。
    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (mYbuffer != null) {
                // reset position, have to be done
                mYbuffer.position(0);
                mUVbuffer.position(0);
                mProg.buildTextures(mYbuffer, mUVbuffer, mVideoWidth, mVideoHeight);
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);//// Redraw background color

                //注意：应用projection转换到绘制对象上时会导致empty display,通常你在projection转换时也要应用camera view转换来让屏幕有东西显示.
                //定义一个Camera View
                // Set the camera position (View matrix)
                Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
                //通过Matrix.setLookAtM()方法来计算camera view转换,然后与之前计算的projection的矩阵进行combine
                // Calculate the projection and view transformation
                Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

                float[] scratch = new float[16];
                if(angle == -1) {
                    Log.d(TAG, "set 0");
                    Matrix.setRotateM(mRotationMatrix, 0, 0, 0, 0, -1.0f);  //固定的角度值
                } else {
                    Log.d(TAG, "set change");
                    Matrix.setRotateM(mRotationMatrix, 0, angle, 0, 0, -1.0f);  //调整角度值
                }

                //将变换矩阵(a rotation matrix)与projection和camera view的转换矩阵combine:
                Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);
                // Set the camera position (View matrix)
                //Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
                // Calculate the projection and view transformation
                //Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
                mProg.drawFrame(scratch);
            }
        }
    }

    private void update(int w, int h) {
        if (w > 0 && h > 0) {
            // adjust ration
            if (mScreenWidth > 0 && mScreenHeight > 0) {
                float f1 = 1f * mScreenHeight / mScreenWidth;
                float f2 = 1f * h / w;
                Log.e(TAG, "f1 = " + f1 + " , f2 = " + f2);
                if (f1 == f2) {
                    mProg.createBuffers(GLProgramUV.squareVertices); //squareVertices  //设置顶点序列
                    Log.i(TAG, "f1 == f2");
                } else if (f1 < f2) {
                    float widScale = f1 / f2;
                    //mProg.createBuffers(new float[] { -widScale, -1.0f, widScale, -1.0f, -widScale, 1.0f, widScale, 1.0f, });  //vert：顶点
                    mProg.createBuffers(new float[] { -widScale, 1.0f, -widScale, -1.0f, widScale, 1.0f, widScale, -1.0f, });
                    Log.i(TAG, "f1 < f2");
                } else {
                    float heightScale = f2 / f1;
                    //mProg.createBuffers(new float[] { -1.0f, -heightScale, 1.0f, -heightScale, -1.0f, heightScale, 1.0f, heightScale, });
                    mProg.createBuffers(new float[] { -1.0f, heightScale, -1.0f, -heightScale, 1.0f, heightScale, 1.0f, -heightScale, });
                    Log.i(TAG, " f1 > f2");
                }
            }
            // init buffer
            if (w != mVideoWidth && h != mVideoHeight) {
                this.mVideoWidth = w;
                this.mVideoHeight = h;
                int yarraySize = w * h;
                int uvarraySize = yarraySize / 2;
                synchronized (this) {
                    mYbuffer = ByteBuffer.allocate(yarraySize);
                    mUVbuffer = ByteBuffer.allocate(uvarraySize);
                }
            }

        }
    }

    /**
     * this method will be called from native code, it's used for passing yuv data to me.
     */
    public void update(byte[] ydata, byte[] uvdata, byte[] vdata) {
        synchronized (this) {
            mYbuffer.clear();
            mUVbuffer.clear();
            mYbuffer.put(ydata, 0, ydata.length);
            mUVbuffer.put(uvdata, 0, uvdata.length);
            Log.e(TAG, "y.length = " + ydata.length + "uv.length = " + uvdata.length);
        }

        // request to render
        //用户调用GLSurfaceView.requestRender()方法时才会调用 onDrawFrame刷新渲染；
        //连续渲染则不依赖于用户调用，GL线程会每隔一段时间自动刷新渲染,连续渲染消耗GPU资源更多，
        //此处为手动渲染
        mTargetSurface.requestRender();
    }

    public void setPreviewSize(int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;
        //mScreenWidth = width;
        //mScreenHeight = height;
        //Log.d(TAG, "w and h: " + mScreenWidth + mScreenHeight );
        update(width, height);
    }
}
