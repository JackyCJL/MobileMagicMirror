package com.example.lenveo.camerademo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Lenveo on 2017/12/3.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final int MEDIA_TYPE_IMAGE = 1;
    private Uri outputMediaFileUri; //记录生成文件的Uri类
    private String outputMediaFileType; //记录生成文件的String类
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private float oldDist = 1f; //初始缩放比例

    public CameraPreview(Context context){
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try{
            c = Camera.open();
        } catch (Exception e){
            Log.d(TAG, "Camera is not available");
        }
        return c;
    }

    private int getPictureSize(List<Camera.Size> sizes) {
        // 屏幕的宽度
        Resources resources = this.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int index = -1;
        for (int i = 0; i < sizes.size(); i++) {
            if (Math.abs(screenWidth - sizes.get(i).width) == 0) {
                index = i;
                break;
                }
            }
        // 当未找到与手机分辨率相等的数值,取列表中间的分辨率
        if (index == -1) {
            index = sizes.size() / 2;
            }
        return index;
    }


    public void surfaceCreated(SurfaceHolder holder){
        mCamera = getCameraInstance();
        try{
           mCamera.setPreviewDisplay(holder);
           mCamera.startPreview();
        } catch(IOException e){
            Log.d(TAG, "Error setting camera preview: "+ e.getMessage());
        }
        Camera.Parameters parameters = mCamera.getParameters();// 获取相机参数集
        /*
        // 设置预览照片的大小
        List<Camera.Size> SupportedPreviewSizes = parameters.getSupportedPreviewSizes();// 获取支持预览照片的尺寸
        Camera.Size previewSize = SupportedPreviewSizes.get(0);// 从List取出Size
        parameters.setPreviewSize(previewSize.width, previewSize.height);//
        */
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();// 获取支持保存图片的尺寸
        //图片大概200KB
        //Camera.Size pictureSize = supportedPictureSizes.get(getPictureSize(supportedPictureSizes));// 从List取出Size
        Camera.Size pictureSize = supportedPictureSizes.get(0);// 从List取出Size 一张大概1.2 1.3MB
        parameters.setPictureSize(pictureSize.width, pictureSize.height);//
        // 设置照片的大小
        mCamera.setParameters(parameters);

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    public  void surfaceChanged(SurfaceHolder holder, int format, int w, int h){

    }

    public File getOutputMediaFile(int type){   //根据参数中指定的文件类型，生成 File 类型的实例，供调用者写入文件
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (!mediaStorageDir.exists()){
            //must be mkdirs(), not mkdir()
            if (!mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
            outputMediaFileType = "image/*";
        }else {
            return null;
        }
        outputMediaFileUri = Uri.fromFile(mediaFile);
        return mediaFile;
    }

    public Uri getOutputMediaFileUri(){
        return outputMediaFileUri;
    }

    public String getOutputMediaFileType(){
        return outputMediaFileType;
    }

    public void takePicture(){
        mCamera.takePicture(null, null, new Camera.PictureCallback(){
            @Override
            public void onPictureTaken(byte[] data, Camera camera){

                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null){
                    Log.d(TAG,"Error creating media file, check storage permissions");
                    return;
                }
                try{
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();

                    camera.startPreview();
                } catch (FileNotFoundException e){
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e){
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                }
            }
        });
    }

    //  对焦相关
    private static Rect calculateTapArea(float x, float y, float coefficient, int width, int height){
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / width * 2000 - 1000);
        int centerY = (int) (y / height * 2000 - 1000);
        int halfAreaSize = areaSize / 2;
        RectF rectF = new RectF(clamp(centerX - halfAreaSize, -1000, 1000),
                                clamp(centerY - halfAreaSize, -1000, 1000),
                                clamp(centerX + halfAreaSize, -1000, 1000),
                                clamp(centerY + halfAreaSize, -1000, 1000));
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private static int clamp(int x, int min, int max){
        if (x > max){
            return max;
        }
        if (x < min){
            return min;
        }
        return x;
    }

    private void handleFocus(MotionEvent event, Camera camera) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f, viewWidth, viewHeight);

        camera.cancelAutoFocus();
        Camera.Parameters params = camera.getParameters();
        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 800));
            params.setFocusAreas(focusAreas);
        } else {
            Log.i(TAG, "focus areas not supported");
        }
        final String currentFocustMode = params.getFocusMode();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        camera.setParameters(params);

        camera.autoFocus(new Camera.AutoFocusCallback() {
        @Override
            public void onAutoFocus(boolean success, Camera camera){
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode(currentFocustMode);
                camera.setParameters(params);
            }
        });

        //触摸测光
        Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f, viewWidth, viewHeight);

        if (params.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 800));
            params.setMeteringAreas(meteringAreas);
        } else {
            Log.i(TAG, "metering areas not supported");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            handleFocus(event, mCamera);
        } else // 手势控制缩放
        {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDist = getFingerSpacing(event);
                    if (newDist > oldDist) {
                        handleZoom(true, mCamera);
                    } else if (newDist < oldDist) {
                        handleZoom(false, mCamera);
                    }
                    oldDist = newDist;
                    break;
            }
        }
        return true;
    }

    //手势缩放
    //得到手指间距
    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    //设置缩放
    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }
}
