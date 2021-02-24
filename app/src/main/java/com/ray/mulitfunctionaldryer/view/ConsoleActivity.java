package com.ray.mulitfunctionaldryer.view;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.component.TimePickerDialog;
import com.ray.mulitfunctionaldryer.component.BottomNavigation;
import com.ray.mulitfunctionaldryer.util.MyApp;
import com.ray.mulitfunctionaldryer.util.RxTimer;

import java.sql.Timestamp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ConsoleActivity extends AppCompatActivity {
    /*******TimePicker*********/
    private final TimePickerDialog dialog = new TimePickerDialog(this);
    private TextView TimerText;
    private final MyApp MyAppInst = MyApp.getAppInstance();

    RadioButton Dryer1RB1;
    RadioButton Dryer1RB2;
    RadioButton Dryer1RB3;
    RadioButton Dryer2RB1;
    RadioButton Dryer2RB2;
    RadioButton Dryer2RB3;
    RadioButton FoggerRB1;
    RadioButton FoggerRB2;
    CheckBox TimerCheck;

    StringBuffer BTMsg = new StringBuffer();
    int Dryer1 = 0;
    int Dryer2 = 0;
    boolean Fogger = false;
    boolean TimeMode = false;
    boolean HeatMode = false;
    public static boolean isTiming = false;
    private String Times = "000";
    Button DryerStartBtn;
    Button DryerStopBtn;

    RxTimer rxTimer = new RxTimer();

    /**
     * @param DeviceType
     * 0 為 未知裝置
     * 1 為 主吹風裝置
     * 2 為 子吹風裝置
     */
    int DeviceType = 0;

    int[] timestamp = new int[4];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);
        BottomNavInit();
        setToolbar();
        TimerPicker();
        initConsole();
        initView();
        initButtonEvent();

        rxTimer.interval(500, number -> {
            if (MyApp.getTimeString().length()>1 && isTiming)
                TimerText.setText(MyApp.getTimeString());
        });
    }

    private void initConsole() {
        TimerText = findViewById(R.id.TimerTV);
        TimerText.setOnClickListener(v -> dialog.showDialog());
        Dryer1RB1 = findViewById(R.id.Dryer1RBtn1);
        Dryer1RB2 = findViewById(R.id.Dryer1RBtn2);
        Dryer1RB3 = findViewById(R.id.Dryer1RBtn3);
        Dryer2RB1 = findViewById(R.id.Dryer2RBtn1);
        Dryer2RB2 = findViewById(R.id.Dryer2RBtn2);
        Dryer2RB3 = findViewById(R.id.Dryer2RBtn3);
        FoggerRB1 = findViewById(R.id.ForRBtnOn);
        FoggerRB2 = findViewById(R.id.ForRBtnOff);
        TimerCheck = findViewById(R.id.TimerCheck);
        DryerStartBtn = findViewById(R.id.DryerStartBtn);
        DryerStopBtn = findViewById(R.id.DryerCancelBtn);
    }

    private void initView() {
        TextView textView = findViewById(R.id.textView5);
        TextView DryerTitle2 = findViewById(R.id.DryerTitle2);
        TextView DryerTitle1 = findViewById(R.id.DryerTitle1);

        DeviceType = MyApp.getDeviceIndex();
        if (DeviceType == 2) {
            Dryer2RB1.setText("開啟");
            Dryer2RB2.setText("關閉");
            Dryer2RB3.setVisibility(View.INVISIBLE);
            textView.setVisibility(View.INVISIBLE);
            DryerTitle2.setText("加熱功能");
            DryerTitle1.setText("吹風裝置段數");
        }
        if (DeviceType == 1) {
            Dryer2RB1.setText("0");
            Dryer2RB2.setText("1");
            Dryer2RB3.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            DryerTitle2.setVisibility(View.VISIBLE);
            DryerTitle1.setText("吹風裝置1 段數");
            DryerTitle2.setText("吹風裝置2 段數");
        }
    }

    private void initButtonEvent() {
        DryerStartBtn.setOnClickListener(v -> {
            try {
                getData();
                setBTMsg(DeviceType);
                MyAppInst.WriteBT(BTMsg.toString());
                MyAppInst.StartCount();
                isTiming = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        DryerStopBtn.setOnClickListener(v -> {
            try {
                isTiming = false;
                timestamp = new int[4];
                MyAppInst.StopCount();
                MyApp.setTimeString("00:00:00");
                MyApp.setTimeSaver(timestamp);
                TimerText.setText("00:00:00");
                MyAppInst.WriteBT("$NND00YN000O");

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void getData() {
        DeviceType = MyApp.getDeviceIndex();
        //DeviceType = 2;
        if (Dryer1RB1.isChecked()) Dryer1 = 0;
        if (Dryer1RB2.isChecked()) Dryer1 = 1;
        if (Dryer1RB3.isChecked()) Dryer1 = 2;
        if (Dryer2RB1.isChecked()) {
            if (DeviceType == 2)
                HeatMode = true;
            else Dryer2 = 0;
        }
        if (Dryer2RB2.isChecked()) {
            if (DeviceType == 2)
                HeatMode = false;
            else Dryer2 = 1;
        }
        if (Dryer2RB3.isChecked()) Dryer2 = 2;
        if (FoggerRB1.isChecked()) Fogger = true;
        if (FoggerRB2.isChecked()) Fogger = false;
        TimeMode = !TimerCheck.isChecked();
    }

    private void setBTMsg(int index) {
        BTMsg = new StringBuffer();
        BTMsg.append('$');
        BTMsg.append('Y');
        if (Fogger && index == 1) BTMsg.append('Y');
        else if (index == 1) BTMsg.append('N');
        BTMsg.append('D');
        BTMsg.append(Dryer1);
        if (index == 1)
            BTMsg.append(Dryer2);
        if (index == 2 && HeatMode)
            BTMsg.append('Y');
        else if (index == 2)
            BTMsg.append('N');
        if (TimeMode) BTMsg.append('Y');
        else BTMsg.append('N');
        BTMsg.append(Times);
        BTMsg.append('O');
    }

    private void BottomNavInit() {
        BottomNavigationView MyBtmNav = findViewById(R.id.Bottom_Main);
        BottomNavigation BtmNav = new BottomNavigation(this, MyBtmNav, 1);
        BtmNav.init();
    }

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("定時模式設定");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    void TimerPicker() {
        final int[] num = new int[4];

        dialog.onDialogRespond = new TimePickerDialog.OnDialogRespond() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onRespond(int times) {
                num[0] = times / 10000;
                num[1] = times % 10000 / 100;
                num[2] = times % 100;
                timestamp = num;
                //TODO Save time value to bluetooth string
            }

            @SuppressLint("DefaultLocale")
            @Override
            public void onResult(boolean ans) {
                if (ans) {
                    String hours = String.format("0%d:", num[0]);
                    String minutes = num[1] >= 10 ? String.format("%d:", num[1]) : String.format("0%d:", num[1]);
                    String seconds = num[2] >= 10 ? String.format("%d", num[2]) : String.format("0%d", num[2]);
                    String times = "";

                    times = times.concat(hours);
                    times = times.concat(minutes);
                    times = times.concat(seconds);

                    int timeIndex = Math.min(num[0] * 3600 + num[1] * 60 + num[2], 999);

                    Times = "";
                    MyApp.setTimeSaver(num);
                    Times = Times.concat(String.valueOf(timeIndex / 100));
                    Times = Times.concat(String.valueOf(timeIndex % 100 / 10));
                    Times = Times.concat(String.valueOf(timeIndex % 10));
                    TimerText.setText(times);
                    System.out.println(num[0]);
                    System.out.println("Sel true");
                } else System.out.println("Sel false");
            }
        };
    }
}
