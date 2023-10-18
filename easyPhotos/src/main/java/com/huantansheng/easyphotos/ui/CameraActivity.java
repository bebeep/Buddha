package com.huantansheng.easyphotos.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hbzhou.open.flowcamera.CustomCameraView;
import com.hbzhou.open.flowcamera.listener.ClickListener;
import com.hbzhou.open.flowcamera.listener.FlowCameraListener;
import com.hbzhou.open.flowcamera.util.LogUtil;
import com.huantansheng.easyphotos.R;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.WhiteBalance;

import java.io.File;

import static com.hbzhou.open.flowcamera.FlowCameraView.BUTTON_STATE_BOTH;

/**
 * 自定义相机  同时拍照和录视频
 */
public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_camera);

        CustomCameraView flowCamera = findViewById(R.id.cameraView);
        //设置生命周期
        flowCamera.setBindToLifecycle(this);
        // 设置白平衡模式
        flowCamera.setWhiteBalance(WhiteBalance.AUTO);
        // 设置只支持单独拍照拍视频还是都支持
        // BUTTON_STATE_ONLY_CAPTURE  BUTTON_STATE_ONLY_RECORDER  BUTTON_STATE_BOTH
        flowCamera.setCaptureMode(BUTTON_STATE_BOTH);
        // 开启HDR
        flowCamera.setHdrEnable(Hdr.ON);
        // 设置最大可拍摄小视频时长
        flowCamera.setRecordVideoMaxTime(60);
        // 设置拍照或拍视频回调监听
        flowCamera.setFlowCameraListener(new FlowCameraListener() {
            @Override
            public void captureSuccess(@NonNull File file) {
                LogUtil.e("photo:"+file.getAbsolutePath());
                Intent intent = new Intent();
                intent.putExtra("imageFile",file);
                setResult(RESULT_OK,intent);
                finish();
            }
            @Override
            public void recordSuccess(@NonNull File file) {
                LogUtil.e("photo："+file.getAbsolutePath());
                Intent intent = new Intent();
                intent.putExtra("videoFile",file);
                setResult(RESULT_OK,intent);
                finish();
            }
            @Override
            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                LogUtil.e("photo拍照/录视频出错:"+message);
            }
        });
        //左边按钮点击事件
        flowCamera.setLeftClickListener(new ClickListener() {
            @Override
            public void onClick() {
                finish();
            }
        });
    }
}
