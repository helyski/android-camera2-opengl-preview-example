package com.tjn.gles.example;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

public class Camera2Wrapper {

    private static final String TAG = "GLCamera_Camera2";

    private Context mContext;
    private CameraManager cameraManager;
    private Surface mPreviewSurface;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraDevice mCameraDevice;

    private ImageReader mImageReader;

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;

    private int mCameraIndex;
    private String mCameraName;

    private int mWidth;
    private int mHeight;

    private MyGLSurfaceView mGLSurfaceView;

    //y总，uv总
    byte[] y_buffer; //= new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT];
    byte[] uv_buffer;// = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT / 2];

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        public void onImageAvailable(ImageReader reader) {
            //TODO
            Image image = reader.acquireNextImage();
            if (null == image) {
                return;
            }
            Log.w(TAG,"onImageAvailable...");

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            ByteBuffer buffer2 = image.getPlanes()[2].getBuffer();

            buffer.get(y_buffer, 0, mWidth * mHeight);
            buffer2.get(uv_buffer, 0, mWidth * mHeight / 2 - 1);

            mGLSurfaceView.update(y_buffer,uv_buffer,null);

            image.close();
        }
    };

    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        }
    };

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG,"CameraDevice.StateCallback onOpened");
            mCameraDevice = camera;
            startPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG,"CameraDevice.StateCallback onDisconnected");
            if(null != mCameraDevice){
                mCameraDevice.close();
                cameraDevice.close();
                mCameraDevice = null;
            }
        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.d(TAG,"CameraDevice.StateCallback onError");
            if(null != mCameraDevice){
                mCameraDevice.close();
                cameraDevice.close();
                mCameraDevice = null;
            }
        }
    };

    public Camera2Wrapper(Context context, int index, String name,int width,int height){
        this.mContext = context;
        this.mCameraIndex = index;
        this.mCameraName = name;
        this.mWidth = width;
        this.mHeight = height;
        // Get CameraManager instance.
        cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        y_buffer = new byte[mWidth * mHeight];
        uv_buffer = new byte[mWidth * mHeight / 2];

    }

    public void setGLSurfaceView(MyGLSurfaceView glSurfaceView){
        this.mGLSurfaceView = glSurfaceView;
    }

    public boolean openCamera(){
        Log.d(TAG,String.format(Locale.CHINA,"openCamera(%d)",mCameraIndex));
        try {
            // Check permission again.
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG,String.format(Locale.CHINA,"openCamera(%d) No Permission!",
                        mCameraIndex));
                return false;
            }
            // Start Camera Thread.
            mCameraThread = new HandlerThread(mCameraName);
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());
            // Open Camera in thread.
            mImageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.YUV_420_888, 20);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);
            cameraManager.openCamera(String.valueOf(mCameraIndex), mStateCallback, mCameraHandler);
            return true;
        }catch (Exception e){
            Log.e(TAG,String.format(Locale.CHINA,"openCamera(%d) catch exception:%s",
                    mCameraIndex,Log.getStackTraceString(e)));
        }
        return false;
    }

    public void startPreview() {
        try {
            Surface imageReaderSurface = mImageReader.getSurface();
            // create CaptureRequestBuilder
            if (mCameraDevice != null) {
                Log.e(TAG, "createCaptureRequest");
                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            } else {
                Log.e(TAG, "createCaptureRequest: mCameraDevice is null");
                return;
            }

            // add surface to CaptureRequestBuilder
            if (imageReaderSurface != null && mPreviewRequestBuilder != null) {
                mPreviewRequestBuilder.addTarget(imageReaderSurface);
            } else {
                Log.e(TAG, "surface is null");
                return;
            }

            // get CameraCaptureSession && start preview
            if (mCameraDevice != null) {
                mCameraDevice.createCaptureSession(Arrays.asList(imageReaderSurface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCaptureSession = session;
                        try {
                            if (mCaptureSession != null && mCameraDevice != null) {
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, null, mCameraHandler);
                            }
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {

                    }
                }, mCameraHandler);
            } else {
                Log.e(TAG, "createCaptureSession: mCameraDevice is null");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void releaseCamera() {
        try {
            if (mPreviewRequestBuilder != null) {
                mPreviewRequestBuilder.removeTarget(mPreviewSurface);
                mPreviewRequestBuilder = null;
            }
            if (mPreviewSurface!= null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }

            if (mCaptureSession != null) {
                mCaptureSession.stopRepeating();
                mCaptureSession.abortCaptures();
                mCaptureSession = null;
            }

            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mPreviewRequest != null) {
                mPreviewRequest = null;
            }

            // stopBackgroundThread
            mCameraThread.quitSafely();
            try {
                mCameraThread.join();
                mCameraThread = null;
                mCameraHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG,String.format(Locale.CHINA,
                        "stopBackgroundThread catch exception:%s",Log.getStackTraceString(e)));
            }
        }catch (Exception e){
            Log.e(TAG,String.format(Locale.CHINA,
                    "Release camera catch exception:%s",Log.getStackTraceString(e)));
        }
    }
}
