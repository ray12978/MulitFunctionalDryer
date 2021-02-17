package com.ray.mulitfunctionaldryer.component;


import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import com.ray.mulitfunctionaldryer.R;

import java.util.Date;

public class TimePickerDialog {
    private Activity activity;
    /**
     * 使InterFace可以被類別使用
     */
    public OnDialogRespond onDialogRespond;

    public TimePickerDialog(Activity activity) {
        this.activity = activity;
    }

    public void showDialog() {
        /**關於AlertDialog相關的設置請參考這篇文章：
         * https://thumbb13555.pixnet.net/blog/post/310777160*/
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.timer_picker_dialog, null);
        mBuilder.setView(view);
        mBuilder.setPositiveButton("確定", null);
        mBuilder.setNegativeButton("取消", null);
        AlertDialog dialog = mBuilder.create();
        /**這裡是設置NumberPicker相關*/
        NumberPicker HourPick, MinutePick, SecondPick;
        HourPick = view.findViewById(R.id.numberPicker_Hour);
        MinutePick = view.findViewById(R.id.numberPicker_Min);
        SecondPick = view.findViewById(R.id.numberPicker_Sec);

        /**設置NumberPicker的最大、最小以及NumberPicker現在要顯示的內容*/

        final String[] SpdList = activity.getResources().getStringArray(R.array.Speed_List);
        HourPick.setMinValue(0);
        HourPick.setMaxValue(6);
        MinutePick.setMinValue(0);
        MinutePick.setMaxValue(59);
        SecondPick.setMinValue(0);
        SecondPick.setMaxValue(59);
        //HourPick.setDisplayedValues(SpdList);
        HourPick.setValue(0); // 設定預設位置
        MinutePick.setValue(0);
        SecondPick.setValue(0);
        HourPick.setWrapSelectorWheel(true); // 是否循環顯示
        MinutePick.setWrapSelectorWheel(true);
        SecondPick.setWrapSelectorWheel(true);
        HourPick.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // 不可編輯
        MinutePick.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        SecondPick.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((v -> {

            /**格式化字串*/
            //String s = String.format("%02d", HourPick.getValue());
            int times = HourPick.getValue()*10000 + MinutePick.getValue() * 100 + SecondPick.getValue();

            /**這邊將值放進OnDialogRespond中*/
            onDialogRespond.onRespond(times);
            try {
                onDialogRespond.onResult(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dialog.dismiss();
        }));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener((v -> {
            try {
                onDialogRespond.onResult(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dialog.dismiss();
        }));
    }

    /**
     * 設置Interface，使取到的直可以被回傳
     */
    public interface OnDialogRespond {
        void onRespond(int time);
        void onResult(boolean ans) throws Exception;
    }
}