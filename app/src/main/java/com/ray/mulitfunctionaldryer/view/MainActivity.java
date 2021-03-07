package com.ray.mulitfunctionaldryer.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.github.ivbaranov.rxbluetooth.predicates.BtPredicate;
import com.github.mikephil.charting.charts.PieChart;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.component.BottomNavigation;
import com.ray.mulitfunctionaldryer.component.WaterPieChart;
import com.ray.mulitfunctionaldryer.util.MyApp;
import com.ray.mulitfunctionaldryer.util.RxTimer;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    /**
     * View
     */
    private WaterPieChart waterPieChart;
    private TextView DryerInfoText, DryerStaText, TempView;
    private ImageView BTLight;

    String TAG = "Dryer";
    /**
     * SharedPreferences
     **/
    private SharedPreferences SensorManager;
    private final MyApp MyAppInst = MyApp.getAppInstance();

    /**
     * Bluetooth
     */
    private String BTAddress, BTName;
    private BluetoothAdapter bluetoothAdapter;

    /**
     * RxJava
     */
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final RxBluetooth rxBluetooth = new RxBluetooth(this);
    private final RxTimer rxTimer1 = new RxTimer();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Initialize();
        timer();
    }

    private void timer() {
        rxTimer1.interval(1000, number -> {
            if (MyApp.getConnected()) UpdateSensorData();
        });
    }

    private void Initialize() {
        SensorManager = getSharedPreferences("SensorVal", MODE_PRIVATE);
        float initWater = SensorManager.getFloat("water", 0);
        int initTemperature = SensorManager.getInt("temp", 0);
        final String deviceName = getSharedPreferences("BTDetail", MODE_PRIVATE)
                .getString("Name", getString(R.string.device_select_not_yet_text));
        BTName = deviceName;
        final String deviceAddress = getSharedPreferences("BTDetail", MODE_PRIVATE)
                .getString("Address", "null");
        BTAddress = deviceAddress;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        TempView = findViewById(R.id.TempTV);
        PieChart pieChart = findViewById(R.id.piechart);
        waterPieChart = new WaterPieChart(pieChart);
        waterPieChart.InitChart();
        waterPieChart.setPieChartValue(initWater);
        TempView.setText(String.valueOf(initTemperature));
        BottomNavInit();
        InitToolbar();
        BTLight = findViewById(R.id.BTStaLight);

        DryerInfoText = findViewById(R.id.DryerInfoTV);
        DryerStaText = findViewById(R.id.DryerStaTV);

        setDryerInfoText(BTName);
        initDryerSta();
        InitEvent();
    }

    private void UpdateSensorData() {

        int TempValue = MyAppInst.getTempIndex();
        int WaterVolume = MyAppInst.getWaterIndex();
        float f = (float) WaterVolume / 750 * 100;

        if (f != 0 && f <= 15 && !MyApp.WaterBeeped) {
            MyAppInst.WaterEmptyNotify();
            MyApp.WaterBeeped = true;
        } else if (f > 15) MyApp.WaterBeeped = false;

        if (TempValue > 30 && !MyApp.TempBeeped) {
            MyAppInst.TempHighNotify();
            MyApp.TempBeeped = true;
        } else if (TempValue < 30) MyApp.TempBeeped = false;
        if (f != 0 && TempValue != 0) {
            waterPieChart.setPieChartValue(f);
            TempView.setText(String.valueOf(TempValue));
            SensorManager.edit()
                    .putFloat("water", f)
                    .putInt("temp", TempValue)
                    .apply();
        }
    }

    private void makeSnack(String msg) {
        Snackbar snackbar = Snackbar.make(DryerInfoText, msg, Snackbar.LENGTH_LONG)
                .setAction("OK", view -> Log.i("SNACKBAR", "OK"));
        snackbar.show();
    }

    @Override
    protected void onStart() {
        getBTStatus();
        if (!MyApp.getConnected()) makeSnack(getString(R.string.select_dryer_first_message));
        super.onStart();
    }

    private void initDryerSta() {
        if (MyApp.getConnected()) {
            DryerStaText.setText(R.string.device_connected);
            BTLight.setImageResource(R.drawable.drawable_circle);
        } else {
            DryerStaText.setText(R.string.device_not_connected);
            BTLight.setImageResource(R.drawable.drawable_circle_gray);
        }
    }

    private void setDryerInfoText(@NonNull String BTname) {
        if (BTname.equals("DryerMain")) {
            DryerInfoText.setText(R.string.main_device_text);
            MyApp.setDeviceIndex(1);
        } else if (BTname.equals("DryerExtend")) {
            DryerInfoText.setText(R.string.extend_device_text);
            MyApp.setDeviceIndex(2);
        } else if (BTname.equals(getString(R.string.device_select_not_yet_text))) {
            DryerInfoText.setText(BTname);
            MyApp.setDeviceIndex(0);
        } else {
            DryerInfoText.setText(R.string.unknown_device_text);
            MyApp.setDeviceIndex(0);
        }
    }

    private void setDryerSta(boolean connected) {
        if (connected) {
            DryerStaText.setText(R.string.device_connected);
            MyApp.isConnected = true;
            BTLight.setImageResource(R.drawable.drawable_circle);
            System.out.println("connected");
        }
        if (!connected) {
            DryerStaText.setText(R.string.device_not_connected);
            MyApp.isConnected = false;
            BTLight.setImageResource(R.drawable.drawable_circle_gray);
            System.out.println("not connected");
            MyApp.isDisconnected();
        }
    }

    private void getBTStatus() {
        MyAppInst.onConnectedDevice = connected -> {
            if (!connected) makeSnack(getString(R.string.bluetooth_conn_failed_text));
        };
    }

    private void BottomNavInit() {
        BottomNavigationView MyBtmNav = findViewById(R.id.Bottom_Main);
        BottomNavigation BtmNav = new BottomNavigation(this, MyBtmNav, 0);
        BtmNav.init();
    }

    private void InitToolbar() {
        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);

        materialToolbar.setOnMenuItemClickListener(item -> {
            int ID = item.getItemId();
            if (ID == R.id.ConnBT) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(intentBluetoothEnable);
                }
                if (BTAddress.equals("") || BTAddress.equals("null")) {
                    makeSnack(getString(R.string.select_correct_device_first_text));
                    return false;
                }
                if (!MyApp.getConnected()) {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BTAddress);
                    MyAppInst.connDevice(device);
                }
            } else if (ID == R.id.DisconnBT)
                if (MyApp.getConnected()) MyAppInst.disconnect();

            return false;
        });
    }

    private void InitEvent() {
        /**
         * get bluetooth Connection State
         */
        compositeDisposable.add(rxBluetooth.observeAclEvent()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(event -> {
                    switch (event.getAction()) {
                        case BluetoothDevice.ACTION_ACL_CONNECTED:
                            Log.e(TAG, "Device is connected");
                            setDryerSta(true);
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                            Log.e(TAG, "Device is disconnected");
                            setDryerSta(false);
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                            Log.e(TAG, "Device is Requested disconnected");
                            break;
                        default:
                            Log.e(TAG, "None Device");
                            setDryerSta(false);
                            break;
                    }
                }));

        compositeDisposable.add(rxBluetooth.observeBluetoothState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.computation())
                .filter(BtPredicate.in(BluetoothAdapter.STATE_ON))
                .subscribe(integer -> initDryerSta()));
        compositeDisposable.add(rxBluetooth.observeBluetoothState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.computation())
                .filter(BtPredicate.in(BluetoothAdapter.STATE_OFF))
                .subscribe(integer -> {
                    setDryerSta(false);
                    DryerStaText.setText(R.string.bluetooth_not_open);
                }));
    }

    public void onBackPressed() {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("要結束應用程式嗎?")
                .setPositiveButton("確定", (dialog, whichButton) -> {
                    finish();//Exit Activity
                })
                .setNegativeButton("取消", (dialog, whichButton) -> {
                }).create().show();
    }
}