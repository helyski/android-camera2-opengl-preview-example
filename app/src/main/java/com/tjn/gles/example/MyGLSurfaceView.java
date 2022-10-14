package com.tjn.gles.example;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

public class MyGLSurfaceView extends GLSurfaceView {

    private Renderer mRenderer;

    public MyGLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        mRenderer = new GLRendererYUV(this);

        this.setEGLContextClientVersion(2);
        this.setRenderer(mRenderer);
        this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


    public void setPreviewSize(int width, int height) {
        if (mRenderer != null) {
            ((GLRendererYUV)mRenderer).setPreviewSize(width, height);
        }
    }

    public void update(byte[] ydata, byte[] udata, byte[] vdata) {
        long t1 = System.currentTimeMillis();
        if (mRenderer != null) {
            ((GLRendererYUV)mRenderer).update(ydata, udata, vdata);
        }
    }


}
