package com.ray.mulitfunctionaldryer.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.github.mikephil.charting.charts.PieChart;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.component.BottomNavigation;
import com.ray.mulitfunctionaldryer.component.WaterPieChart;
import com.ray.mulitfunctionaldryer.util.MyApp;
import com.ray.mulitfunctionaldryer.util.RxTimer;
import com.ray.mulitfunctionaldryer.util.RxWaterTimer;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    public StringBuffer BTSendMsg = new StringBuffer("$NND00N000O");
    private final int BT_MSG_LEN = 11;

    /**
     * View
     */
    private WaterPieChart waterPieChart;
    private TextView DryerInfoText, DryerStaText, TempView;
    private MaterialToolbar materialToolbar;
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
    private boolean isConnected;

    /**
     * RxJava
     */
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final RxBluetooth rxBluetooth = new RxBluetooth(this);
    private final RxTimer rxTimer1 = new RxTimer();
    private final RxTimer rxTimer2 = new RxTimer();
    /**
     * Testing saveInstance
     */
    String StrTest;
    int IntTest;

    //todo add button to test write bluetooth strings


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (null != savedInstanceState) {//因為Activity的生命周期原因 ，if 語句放著不一定能執行得到 應該結合實際情況

            IntTest = savedInstanceState.getInt("IntTest");

            StrTest = savedInstanceState.getString("StrTest");
            System.out.println("getting");
        }
        setContentView(R.layout.activity_main);
        Initialize();

        Log.d("Act", "onCreate");
        System.out.println("String:" + StrTest);
        System.out.println("Int:" + IntTest);
        timer();
    }

    void timer() {
        AtomicInteger i = new AtomicInteger();
        AtomicInteger a = new AtomicInteger(5);
        rxTimer1.interval(1000, number -> {
            if (MyApp.getConnected()) UpdateSensorData();
        });
        rxTimer2.interval(2000, number -> {
            System.out.println(a.getAndIncrement());
        });
    }

    private void Initialize() {
        SensorManager = getSharedPreferences("SensorVal", MODE_PRIVATE);
        float initWater = SensorManager.getFloat("water", 0);
        int initTemperature = SensorManager.getInt("temp", 0);
        final String deviceName = getSharedPreferences("BTDetail", MODE_PRIVATE)
                .getString("Name", "尚未選擇裝置");
        final String deviceAddress = getSharedPreferences("BTDetail", MODE_PRIVATE)
                .getString("Address", "null");
        BTAddress = deviceAddress;
        BTName = deviceName;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        TempView = findViewById(R.id.TempTV);
        PieChart pieChart = findViewById(R.id.piechart);
        waterPieChart = new WaterPieChart(pieChart);
        waterPieChart.InitChart();
        waterPieChart.setPieChartValue(initWater);//test
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

    void UpdateSensorData() {

        int TempValue = MyAppInst.getTempIndex();
        Integer WaterVolume = MyAppInst.getWaterIndex();
        System.out.println(WaterVolume);
        float f = WaterVolume.floatValue() / 750 * 100;
        if (f != 0 && TempValue != 0) {
            waterPieChart.setPieChartValue(f);
            TempView.setText(String.valueOf(TempValue));
            SensorManager.edit()
                    .putFloat("water", f)
                    .putInt("temp", TempValue)
                    .apply();
        }

        System.out.println("update!");

    }

    private void makeSnack(String msg) {
        Snackbar snackbar = Snackbar.make(DryerInfoText, msg, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.i("SNACKBAR", "OK");
                    }
                });
        snackbar.show();
    }

    @Override
    protected void onStart() {
        setBTStatus();
        if (!MyApp.getConnected()) makeSnack("請先連線吹風裝置");
        super.onStart();
    }

    private void initDryerSta() {
        if (MyApp.getConnected()) {
            DryerStaText.setText("吹風裝置已連線");
            BTLight.setImageResource(R.drawable.drawable_circle);
        } else {
            DryerStaText.setText("吹風裝置尚未連線");
            BTLight.setImageResource(R.drawable.drawable_circle_gray);
        }
    }

    private void setDryerInfoText(@NonNull String BTname) {
        if (BTname.equals("DryerMain")){
            DryerInfoText.setText("主吹風裝置");
            MyApp.setDeviceIndex(1);
        }

        else if (BTname.equals("DryerExtend")){
            DryerInfoText.setText("子吹風裝置");
            MyApp.setDeviceIndex(2);
        }

        else{
            DryerInfoText.setText("不支援的裝置");
            MyApp.setDeviceIndex(0);
        }
    }

    private void setDryerSta(boolean connected) {
        if (connected) {
            DryerStaText.setText("吹風裝置已連線");
            isConnected = true;
            BTLight.setImageResource(R.drawable.drawable_circle);
            System.out.println("connected");
        }
        if (!connected) {
            DryerStaText.setText("吹風裝置尚未連線");
            isConnected = false;
            BTLight.setImageResource(R.drawable.drawable_circle_gray);
            System.out.println("not connected");
            MyApp.isDisconnected();
        }
    }

    private void setBTStatus() {
        MyAppInst.onConnectedDevice = ans -> {
            isConnected = ans;
        };
    }

    private void BottomNavInit() {
        BottomNavigationView MyBtmNav = findViewById(R.id.Bottom_Main);
        BottomNavigation BtmNav = new BottomNavigation(this, MyBtmNav, 0);
        BtmNav.init();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d("Act", "onPrepareOptionsMenu");
        if (isConnected)
            menu.findItem(R.id.ConnBT).setIcon(R.drawable.drawable_bluetooth_connected);
        if (!isConnected) menu.findItem(R.id.ConnBT).setIcon(R.drawable.drawable_bluetooth_white);
        return super.onPrepareOptionsMenu(menu);
    }

    private void InitToolbar() {
        materialToolbar = findViewById(R.id.topAppBar);

        materialToolbar.setOnMenuItemClickListener(item -> {
            int ID = item.getItemId();
            if (ID == R.id.ConnBT) {
                if (!BTAddress.equals("")) {
                    System.out.println(BTAddress);
                    if (!isConnected) item.setIcon(R.drawable.drawable_bluetooth_connected);
                    else item.setIcon(R.drawable.drawable_bluetooth_white);

                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BTAddress);
                    MyAppInst.connDevice(device);
                }
            } else if (ID == R.id.PageRefresh) {

                UpdateSensorData();
            }
            return false;
        });
        // materialToolbar.setMenu();
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
                            //DpBTConnState(true);
                            setDryerSta(true);
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                            Log.e(TAG, "Device is disconnected");
                            //DpBTConnState(false);
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
    }
}