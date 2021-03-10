package com.ray.mulitfunctionaldryer.view;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.component.TimePickerDialog;
import com.ray.mulitfunctionaldryer.component.BottomNavigation;
import com.ray.mulitfunctionaldryer.util.MyApp;
import com.ray.mulitfunctionaldryer.util.RxTimer;

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
    RadioButton Dryer2RB1;
    RadioButton Dryer2RB2;
    RadioButton Dryer2RB3;
    RadioButton Dryer3RB1;
    RadioButton Dryer3RB2;
    RadioButton Dryer3RB3;
    RadioButton FoggerRB1;
    RadioButton FoggerRB2;
    RadioButton ExDryerRB1;
    RadioButton ExDryerRB2;
    RadioButton HeatRB1;
    RadioButton HeatRB2;
    RadioButton HeatRB3;
    RadioButton HeatRB4;
    CheckBox TimerCheck;

    StringBuffer BTMsg = new StringBuffer();
    int Dryer1 = 0;
    int Dryer2 = 0;
    int Dryer3 = 0;
    boolean Fogger = false;
    boolean TimeMode = false;
    int HeatMode = 0;
    boolean ExtendFanMode = false;
    public static boolean isTiming = false;
    public static boolean TimeModeNotify = false;
    public static String Times = "000";
    Button DryerStartBtn;
    Button DryerStopBtn;

    RxTimer rxTimer = new RxTimer();

    /**
     * @param DeviceType
     * 0 為 未知裝置
     * 1 為 主吹風裝置
     * 2 為 子吹風裝置
     */
    int DeviceType = 1;

    int[] timestamp = new int[4];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);
        BottomNavInit();
        TimerPicker();
        initConsole();
        initView();
        initButtonEvent();
        setToolbar();
        CounterStart();
    }


    void CounterStart() {
        rxTimer.interval(500, number -> {
            if (MyApp.getTimeString().length() > 1 && isTiming) {
                TimerText.setText(MyApp.getTimeString());
                System.out.print("計時:");
                System.out.println(MyApp.getTimeString());
                if (MyApp.TimerString.equals("00:00:00")) {
                    isTiming = false;
                    MyAppInst.StopCount();
                    TimerText.setText(MyApp.TimerString);
                }
                if (TimeModeNotify) {
                    if (DeviceType == 1) makeSnack(getString(R.string.main_time_mode_start));
                    if (DeviceType == 2) makeSnack(getString(R.string.ex_time_mode_start));
                    TimeModeNotify = false;
                }
            }
            if (MyApp.getTimeString().length() > 1 && !isTiming)
                TimerText.setText(MyApp.TimerString);
        });
    }

    private boolean VerifyMainDeviceSett() {
        boolean ans = true;
        return ans;
    }

    private boolean VerifyExtendDeviceSett() {
        boolean ans = true;
        if (DeviceType == 2) {
            if (HeatMode != 0 && !ExtendFanMode) ans = false;
        }
        return ans;
    }

    private void initConsole() {
        TimerText = findViewById(R.id.TimerTV);
        TimerText.setOnClickListener(v -> {
            if (!TimerCheck.isChecked()) dialog.showDialog();
            else if (TimerCheck.isChecked()) makeSnack("已停用定時功能");
        });
        //Type1
        Dryer1RB1 = findViewById(R.id.Dryer1RBtn1);
        Dryer1RB2 = findViewById(R.id.Dryer1RBtn2);
        Dryer2RB1 = findViewById(R.id.Dryer2RBtn1);
        Dryer2RB2 = findViewById(R.id.Dryer2RBtn2);
        Dryer2RB3 = findViewById(R.id.Dryer2RBtn3);
        Dryer3RB1 = findViewById(R.id.Dryer3RBtn1);
        Dryer3RB2 = findViewById(R.id.Dryer3RBtn2);
        Dryer3RB3 = findViewById(R.id.Dryer3RBtn3);
        FoggerRB1 = findViewById(R.id.ForRBtnOn);
        FoggerRB2 = findViewById(R.id.ForRBtnOff);

        //Type2
        HeatRB1 = findViewById(R.id.HeatRBtn1);
        HeatRB2 = findViewById(R.id.HeatRBtn2);
        HeatRB3 = findViewById(R.id.HeatRBtn3);
        HeatRB4 = findViewById(R.id.HeatRBtn4);
        ExDryerRB1 = findViewById(R.id.ExDryerRBtn1);
        ExDryerRB2 = findViewById(R.id.ExDryerRBtn2);


        TimerCheck = findViewById(R.id.TimerCheck);
        DryerStartBtn = findViewById(R.id.DryerStartBtn);
        DryerStopBtn = findViewById(R.id.DryerCancelBtn);

        DeviceType = MyApp.getDeviceIndex();
    }

    private void initView() {
        TextView DryerTitle3 = findViewById(R.id.DryerTitle3);
        TextView DryerTitle2 = findViewById(R.id.DryerTitle2);
        TextView DryerTitle1 = findViewById(R.id.DryerTitle1);
        TextView FoggerTitle = findViewById(R.id.textView6);
        TextView HeatTitle = findViewById(R.id.HeatTitle);
        TextView ExDryerTitle = findViewById(R.id.ExDryerTitle);
        DeviceType = MyApp.getDeviceIndex();
        if (DeviceType == 2) { //若為子裝置
            HeatTitle.setVisibility(View.VISIBLE);
            ExDryerTitle.setVisibility(View.VISIBLE);
            HeatRB1.setVisibility(View.VISIBLE);
            HeatRB2.setVisibility(View.VISIBLE);
            HeatRB3.setVisibility(View.VISIBLE);
            HeatRB4.setVisibility(View.VISIBLE);
            ExDryerRB1.setVisibility(View.VISIBLE);
            ExDryerRB2.setVisibility(View.VISIBLE);


            Dryer1RB1.setVisibility(View.INVISIBLE);
            Dryer1RB2.setVisibility(View.INVISIBLE);
            Dryer2RB1.setVisibility(View.INVISIBLE);
            Dryer2RB2.setVisibility(View.INVISIBLE);
            Dryer2RB3.setVisibility(View.INVISIBLE);
            Dryer3RB1.setVisibility(View.INVISIBLE);
            Dryer3RB2.setVisibility(View.INVISIBLE);
            Dryer3RB3.setVisibility(View.INVISIBLE);
            DryerTitle1.setVisibility(View.INVISIBLE);
            DryerTitle2.setVisibility(View.INVISIBLE);
            DryerTitle3.setVisibility(View.INVISIBLE);
            FoggerTitle.setVisibility(View.INVISIBLE);
            FoggerRB1.setVisibility(View.INVISIBLE);
            FoggerRB2.setVisibility(View.INVISIBLE);
        }
        if (DeviceType == 1 || DeviceType == 0) { //若為主裝置
            HeatTitle.setVisibility(View.INVISIBLE);
            ExDryerTitle.setVisibility(View.INVISIBLE);
            HeatRB1.setVisibility(View.INVISIBLE);
            HeatRB2.setVisibility(View.INVISIBLE);
            HeatRB3.setVisibility(View.INVISIBLE);
            HeatRB4.setVisibility(View.INVISIBLE);
            ExDryerRB1.setVisibility(View.INVISIBLE);
            ExDryerRB2.setVisibility(View.INVISIBLE);


            Dryer1RB1.setVisibility(View.VISIBLE);
            Dryer1RB2.setVisibility(View.VISIBLE);
            Dryer2RB1.setVisibility(View.VISIBLE);
            Dryer2RB2.setVisibility(View.VISIBLE);
            Dryer2RB3.setVisibility(View.VISIBLE);
            Dryer3RB1.setVisibility(View.VISIBLE);
            Dryer3RB2.setVisibility(View.VISIBLE);
            Dryer3RB3.setVisibility(View.VISIBLE);
            DryerTitle1.setVisibility(View.VISIBLE);
            DryerTitle2.setVisibility(View.VISIBLE);
            DryerTitle3.setVisibility(View.VISIBLE);
            FoggerTitle.setVisibility(View.VISIBLE);
            FoggerRB1.setVisibility(View.VISIBLE);
            FoggerRB2.setVisibility(View.VISIBLE);


        }
    }

    private void initButtonEvent() {
        DryerStartBtn.setOnClickListener(v -> {
            try {
                getData();
                setBTMsg(DeviceType);
                System.out.println(BTMsg.toString());
                if (!VerifyExtendDeviceSett()) {
                    makeSnack(getString(R.string.open_dryer_and_heat_warn));
                } else {
                    if (!MyApp.getConnected()) makeSnack(getString(R.string.bluetooth_lost_warn));
                    else if (BTMsg.length() == 0) makeSnack(getString(R.string.lost_setting_warn));
                    else if (TimeMode) {
                        if (Times.length() == 0 || Times.equals("000"))
                            makeSnack(getString(R.string.time_lost_warn));
                        else MyAppInst.WriteBT(BTMsg.toString());
                    } else {
                        MyAppInst.WriteBT(BTMsg.toString());
                        //MyAppInst.StartCount();
                        //isTiming = true;
                    }

                    //test
                    /*isTiming = true;
                    MyAppInst.StartCount();*/

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        DryerStopBtn.setOnClickListener(v -> {
            try {
                isTiming = false;
                timestamp = new int[4];
                MyAppInst.StopCount();
                MyApp.setTimeString(getString(R.string.timer_default_value));
                MyApp.setTimeSaver(timestamp);
                TimerText.setText(getString(R.string.timer_default_value));
                if (DeviceType == 1)
                    MyAppInst.WriteBT(getString(R.string.bluetooth_main_cancel_text));
                if (DeviceType == 2)
                    MyAppInst.WriteBT(getString(R.string.bluetooth_extend_cancel_text));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void getData() {
        if (DeviceType == 1) {
            if (Dryer1RB1.isChecked()) Dryer1 = 0;
            if (Dryer1RB2.isChecked()) Dryer1 = 1;
            if (Dryer2RB1.isChecked()) Dryer2 = 0;
            if (Dryer2RB2.isChecked()) Dryer2 = 1;
            if (Dryer2RB3.isChecked()) Dryer2 = 2;
            if (Dryer3RB1.isChecked()) Dryer3 = 0;
            if (Dryer3RB2.isChecked()) Dryer3 = 1;
            if (Dryer3RB3.isChecked()) Dryer3 = 2;
            if (FoggerRB1.isChecked()) Fogger = true;
            if (FoggerRB2.isChecked()) Fogger = false;
        } else if (DeviceType == 2) {
            if (HeatRB1.isChecked()) HeatMode = 0;
            if (HeatRB2.isChecked()) HeatMode = 1;
            if (HeatRB3.isChecked()) HeatMode = 2;
            if (HeatRB4.isChecked()) HeatMode = 3;
            if (ExDryerRB1.isChecked()) ExtendFanMode = true;
            if (ExDryerRB2.isChecked()) ExtendFanMode = false;
        }
        TimeMode = !TimerCheck.isChecked();
    }

    private void setBTMsg(int index) {
        BTMsg = new StringBuffer();
        BTMsg.append('$');
        BTMsg.append('Y');
        if (index == 1) {
            if (Fogger) BTMsg.append('Y');
            else BTMsg.append('N');
            BTMsg.append('D');
            BTMsg.append(Dryer1);
            BTMsg.append(Dryer2);
            BTMsg.append(Dryer3);
        }
        if (index == 2) {
            BTMsg.append('D');
            BTMsg.append(HeatMode);
            if (ExtendFanMode) BTMsg.append('Y');
            else BTMsg.append('N');
        }
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
        if (DeviceType == 0) toolbar.setTitle(getString(R.string.console_toolbar_title_unknown));
        if (DeviceType == 1) toolbar.setTitle(getString(R.string.console_toolbar_title_main));
        if (DeviceType == 2) toolbar.setTitle(getString(R.string.console_toolbar_title_extend));
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
                    MyApp.setTimeString(times);
                    TimerText.setText(times);
                    System.out.println(num[0]);
                    System.out.println("Sel true");
                } else System.out.println("Sel false");
            }
        };
    }

    private void makeSnack(String msg) {
        Snackbar snackbar = Snackbar.make(TimerText, msg, Snackbar.LENGTH_LONG)
                .setAction("OK", view -> Log.i("SNACKBAR", "OK"));
        snackbar.show();
    }
}
