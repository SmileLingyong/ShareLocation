package com.example.smile.shareloaction;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Toast;


/**
 * Created by lly54 on 2017/10/15.
 * Clicking StartLocationButton.
 */


public class StartLocationDialog extends PopupWindow {

    private Button singleLocationBtnDialog;
    private Button realTimeLocationBtnDialog;
    private View mMenuView;

    // 注意：这里我们为了测试方便，就没有写三个参数的构造函数。具体参考BaiduTrack项目
    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    public StartLocationDialog(final Activity context) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuView = inflater.inflate(R.layout.dialog_start_location, null);
        singleLocationBtnDialog = (Button) mMenuView.findViewById(R.id.btn_dialog_single_loc);
        realTimeLocationBtnDialog = (Button) mMenuView.findViewById(R.id.btn_dialog_real_time_loc);

        this.setContentView(mMenuView);
        this.setWidth(LayoutParams.MATCH_PARENT);
        this.setHeight(LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);
        this.setAnimationStyle(R.style.user_center_anim_style);
        ColorDrawable dw = new ColorDrawable(0xb0000000);
        this.setBackgroundDrawable(dw);
        mMenuView.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();  // 销毁弹出框
                    }
                }
                return true;
            }
        });

        // 点击单次共享
        singleLocationBtnDialog.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "单次共享", Toast.LENGTH_SHORT).show();
                dismiss();  // 销毁弹出框
            }
        });

        // 点击实时共享
        realTimeLocationBtnDialog.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "实时共享", Toast.LENGTH_SHORT).show();
                dismiss();  // 销毁弹出框
            }
        });


    }


}
