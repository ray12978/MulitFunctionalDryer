package com.ray.mulitfunctionaldryer.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toolbar;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.component.BottomNavigation;
import com.ray.mulitfunctionaldryer.component.WaterPieChart;
import com.ray.mulitfunctionaldryer.util.MyApp;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public StringBuffer BTSendMsg = new StringBuffer("$NND00N000O"); //[0]StartBit[1]Lock{L,F,N},[2]SpeedTen,[3]SpeedUnit,[4]SpeedConfirm,[5]Laser{T,J,N},[6]Buzzer{E,N},[7]CloudMode{Y,N}
    private final int BT_MSG_LEN = 11;
    private WaterPieChart waterPieChart;
    private TextView DryerInfoText;


    String TAG = "Dryer";
    /**
     * SharedPreferences
     **/
    private SharedPreferences BTWrData;
    private final MyApp MyAppInst = MyApp.getAppInstance();

    /**
     * Bluetooth
     */
    private String BTAddress, BTName;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Initialize();


    }

    private void Initialize() {
        BTWrData = getSharedPreferences("BTMsg", MODE_PRIVATE);
        final String deviceName = getSharedPreferences("BTDetail", MODE_PRIVATE)
                .getString("Name", "尚未選擇裝置");
        final String deviceAddress = getSharedPreferences("BTDetail", MODE_PRIVATE)
                .getString("Address", "null");
        BTAddress = deviceAddress;
        BTName = deviceName;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        PieChart pieChart = (PieChart) findViewById(R.id.piechart);
        waterPieChart = new WaterPieChart(pieChart);
        waterPieChart.InitChart();
        waterPieChart.setPieChartValue(35.8f);//test

        BottomNavInit();
        InitToolbar();

        DryerInfoText = findViewById(R.id.DryerInfoTV);
        setDryerInfo(BTName);


    }

    private void setDryerInfo(@NonNull String BTname){
        if(BTname.equals("MainDryer"))
            DryerInfoText.setText("主吹風裝置");
        if(BTname.equals(""))//TODO change condition
            DryerInfoText.setText("副吹風裝置");
    }

    private void BottomNavInit() {
        BottomNavigationView MyBtmNav = findViewById(R.id.Bottom_Main);
        BottomNavigation BtmNav = new BottomNavigation(this, MyBtmNav, 0);
        BtmNav.init();
    }

    void UpdateBTMsg() {
        BTWrData.edit()
                .putString("SendMsg", BTSendMsg.toString())
                .apply();
    }

    private void InitToolbar(){
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);

        materialToolbar.setOnMenuItemClickListener(item -> {
            int ID = item.getItemId();
            if(ID == R.id.ConnBT){
                Log.d(TAG,"ConnBT");
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BTAddress);
                MyAppInst.connDevice(device);
            }else if (ID == R.id.PageRefresh){
                Log.d(TAG,"PageRefresh");
            }
            return false;
        });
    }
}