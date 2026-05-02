package com.homney.app.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.homney.app.R;

public class LoadingDialog {

    private Dialog dialog;
    private Context context;

    public LoadingDialog(Context context) {
        this.context = context;
    }

    public void show() {
        if (dialog == null) {
            dialog = new Dialog(context);
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
            dialog.setContentView(view);
            dialog.setCancelable(false);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            ImageView ivLoading = view.findViewById(R.id.iv_loading_logo);
            Animation rotate = AnimationUtils.loadAnimation(context, R.anim.rotate_indefinitely);
            ivLoading.startAnimation(rotate);
        }
        dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
