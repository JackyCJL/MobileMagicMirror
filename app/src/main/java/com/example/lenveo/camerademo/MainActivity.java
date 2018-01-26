package com.example.lenveo.camerademo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.IOException;

/**
 * Created by Lenveo on 2017/12/3.
 */

public class MainActivity extends Activity{
    private  CameraPreview mPreview;
    private  Camera mCamera;
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //  动态申请权限 CAMERA & WRITE_TXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0/*requestCode*/);
        }
        //while (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {}
        //isGrantExternalRW(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10/*requestCode*/);

        }

        setContentView(R.layout.activity_main);

        initCamera();
        /*
        int hasWriteContactsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        */
        final Button buttonCapturePhoto = (Button) findViewById(R.id.takepicture);
        buttonCapturePhoto.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mPreview.takePicture();
               Log.i("MainActivity", "take pictake");
            }
        });
        final Button buttonChangeCamera = (Button) findViewById(R.id.change);
        buttonChangeCamera.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try{
                    mPreview.changeCamera();
                } catch (IOException e){
                    Log.e("MainActivity", "Change Camera Error");
                }

            }
        });

        final Button buttonPicture = (Button) findViewById(R.id.picture);
        buttonPicture.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
                startActivity(intent);
            }
        });


    }

    private void initCamera(){

        mPreview = new CameraPreview(this );
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        mCamera = mPreview.getCamera();
        preview.addView(mPreview);

    }
/*
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();

        mCamera.release();
        mCamera = null;
    }
*/
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (mCamera == null){
            mCamera = CameraPreview.getCameraInstance();
        }
        //必须放在onResume中，不然会出现Home键之后，再回到该APP，黑屏
        mPreview = new CameraPreview(this );
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        mCamera = mPreview.getCamera();
        preview.addView(mPreview);
    }


}
