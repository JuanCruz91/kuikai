// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.demo.objectdetection;

import android.Manifest;
import android.content.pm.PackageManager;


import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.view.TextureView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
/**
 * https://developer.android.com/training/camerax/architecture?hl=es-419
 * @param <E>
 */
public abstract class AbstractCameraXActivity<E> extends BaseModuleActivity  {
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};

    private SeekBar mSeekBar, mSeekBarIOU, mSeekBarNumObjects;
    private TextView mtextViewSeekBar, mtextViewSeekBarIOU, mtextViewNumObjects;
    private long mLastAnalysisResultTime;
    private float mSeekbarProgress, mSeekbarProgressIOU;
    private int mSeekbarProgressNumObjects;


    /**
     * Method abstract
     * @return
     */
    protected abstract int getContentViewLayoutId();

    protected abstract TextureView getCameraPreviewTextureView();

    /**
     * On Create:
     * StartBackgroundThread()
     * Request permissions and setupCameraX()
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewLayoutId());

        startBackgroundThread();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            setupCameraX();
        }

        /**
         * The Seekbar control the threshold. It need a listener to change the value.
         * The SeekbarIOU control the IOUthreshold. It need a listener to change the value.
         */
        mSeekBarIOU = findViewById(org.pytorch.demo.objectdetection.R.id.seekBarIOU);
        mSeekBar = findViewById(org.pytorch.demo.objectdetection.R.id.seekBar);
        mSeekBarNumObjects=findViewById(R.id.seekBarNumObjects );

        mtextViewSeekBar = findViewById(org.pytorch.demo.objectdetection.R.id.textViewSeekBar);
        mtextViewSeekBarIOU = findViewById(org.pytorch.demo.objectdetection.R.id.textViewSeekBarIOU);
        mtextViewNumObjects = findViewById(R.id.textViewSeekBarnumObjects);

        mtextViewSeekBar.setText("Conf: "+ (String.format(" %.2f", ((float)mSeekBar.getProgress())/100)));
        mtextViewSeekBarIOU.setText("IOU: "+ (String.format(" %.2f",((float)mSeekBarIOU.getProgress())/100)));
        mtextViewNumObjects.setText("Quantity: "+ mSeekBarNumObjects.getProgress());


        /**
         * IOU Bar
         */
        mSeekBarIOU.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekbarProgressIOU = progress;
                mSeekbarProgressIOU = mSeekbarProgressIOU / 100;
                if (seekBar == mSeekBarIOU) {
                    mtextViewSeekBarIOU.setText("IOU: " + (String.format(" %.2f",mSeekbarProgressIOU)));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar == mSeekBarIOU) {
                    PrePostProcessor.setmThresholdIOU(mSeekbarProgressIOU);
                }
            }
        });

        /**
         * Conf Bar
         */
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekbarProgress = progress;
                mSeekbarProgress = mSeekbarProgress / 100;
                mtextViewSeekBar.setText("Conf: " + (String.format(" %.2f",mSeekbarProgress)));
                }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                PrePostProcessor.setmThreshold(mSeekbarProgress);
            }
        });

        /**
         * numObjects bar
         */
        mSeekBarNumObjects.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekbarProgressNumObjects = progress;
                mtextViewNumObjects.setText("Quantity: " + progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                PrePostProcessor.setmNmsLimit(mSeekbarProgressNumObjects);
            }
        });
    }

    /**
     * request permissions
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "You can't use object detection example without granting CAMERA permission", Toast.LENGTH_LONG).show();
                finish();
            } else {
                setupCameraX();
            }
        }
    }

    /**
     * Camera's settings
     */
    private void setupCameraX() {

        /**TextureVIew: Muestra el flujo de contenido*/
        final TextureView textureView = getCameraPreviewTextureView();
        final PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(720,1080))
//              .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .setCallbackHandler(mBackgroundHandler)
                .build();

        final Preview preview = new Preview(previewConfig);
        
        preview.setOnPreviewOutputUpdateListener(output -> textureView.setSurfaceTexture(output.getSurfaceTexture()));

        /**
         *Camera's settings
         */
        final ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                    /**Cambio en la resolución: ORIGINAL -> 480, 640
                     * Cambio de la resolución 2: 520, 720*/
                .setTargetResolution(new Size(720 , 1080))
                .setCallbackHandler(mBackgroundHandler)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .build();

        final ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer((image, rotationDegrees) -> {

            if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 500) {
                return;
            }

            final E result = analyzeImage(image, rotationDegrees);

            if (result != null) {
                mLastAnalysisResultTime = SystemClock.elapsedRealtime();
                runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
            }

        });
        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    @WorkerThread
    @Nullable
    protected abstract E analyzeImage(ImageProxy image, int rotationDegrees);

    @UiThread

    protected abstract void applyToUiAnalyzeImageResult(E result);
}