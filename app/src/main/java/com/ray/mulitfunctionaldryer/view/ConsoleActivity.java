package com.ray.mulitfunctionaldryer.view;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.component.TimePickerDialog;
import com.ray.mulitfunctionaldryer.component.BottomNavigation;
import com.ray.mulitfunctionaldryer.util.MyApp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ConsoleActivity extends AppCompatActivity {
    /*******TimePicker*********/
    private final TimePickerDialog dialog = new TimePickerDialog(this);
    private TextView TimerText;
    private MyApp MyAppInst = MyApp.getAppInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);
        BottomNavInit();
        setToolbar();
        TimerPicker();
        InitConsole();
    }

    private void InitConsole() {
        TimerText = findViewById(R.id.TimerTV);
        TimerText.setOnClickListener(v -> dialog.showDialog());
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
            @Override
            public void onRespond(int times) {
                num[0] = times / 10000;
                num[1] = times % 10000 / 100;
                num[2] = times % 100;
                int timeSet = Math.min(times, 999);
                //TODO Save time value to bluetooth string

            }

            @SuppressLint("DefaultLocale")
            @Override
            public void onResult(boolean ans) {
                if (ans) {
                    if (num == null) return;
                    String hours = String.format("0%d:", num[0]);
                    String minutes = num[1] >= 10 ? String.format("%d:", num[1]) : String.format("0%d:", num[1]);
                    String seconds = num[2] >= 10 ? String.format("%d", num[2]) : String.format("0%d", num[2]);
                    String times = "";

                    times = times.concat(hours);
                    times = times.concat(minutes);
                    times = times.concat(seconds);

                    TimerText.setText(times);
                    System.out.println(num[0]);
                    System.out.println("Sel true");
                } else System.out.println("Sel false");
            }
        };
    }
}
