package com.huantansheng.easyphotos.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.huantansheng.easyphotos.R;

public class CameraTypeDialog extends BottomSheetDialog {

    public interface OnButtonClickListener{
        void onClick(View view);
    }

    private OnButtonClickListener onButtonClickListener;


    public void setOnButtonClickListener(OnButtonClickListener onButtonClickListener){
        this.onButtonClickListener = onButtonClickListener;
    }


    public CameraTypeDialog(@NonNull Context context) {
        super(context,R.style.bottom_sheet_dialog);
        init(context);
    }



    private void init(Context context){
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_camera_type,null);
        view.findViewById(R.id.tv_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClickListener.onClick(v);
            }
        });
        view.findViewById(R.id.btn_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClickListener.onClick(v);
            }
        });
        view.findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClickListener.onClick(v);
            }
        });
        setContentView(view);
    }
}
