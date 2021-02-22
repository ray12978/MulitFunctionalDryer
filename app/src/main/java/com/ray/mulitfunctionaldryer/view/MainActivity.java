package com.ray.mulitfunctionaldryer.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toolbar;

import com.github.ivbaranov.rxbluetooth.RxBluetooth;
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

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.ray.mulitfunctionaldryer.util.MyApp.getConnected;

public class MainActivity extends AppCompatActivity {
    public StringBuffer BTSendMsg = new StringBuffer("$NND00N000O");
    private final int BT_MSG_LEN = 11;

    /**
     * View
     */
    private WaterPieChart waterPieChart;
    private TextView DryerInfoText, DryerStaText;
    private MaterialToolbar materialToolbar;


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
    private boolean isConnected;

    /**
     * RxJava
     */
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final RxBluetooth rxBluetooth = new RxBluetooth(this);

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

        PieChart pieChart = findViewById(R.id.piechart);
        waterPieChart = new WaterPieChart(pieChart);
        waterPieChart.InitChart();
        waterPieChart.setPieChartValue(35.8f);//test

        BottomNavInit();
        InitToolbar();

        DryerInfoText = findViewById(R.id.DryerInfoTV);
        DryerStaText = findViewById(R.id.DryerStaTV);
        if(MyApp.getConnected()) DryerStaText.setText("吹風裝置已連線");
        else DryerStaText.setText("吹風裝置尚未連線");
        setDryerInfoText(BTName);

        InitEvent();
    }

    @Override
    protected void onStart() {
        setBTStatus();
        Log.d("Act", "onStart");
        super.onStart();
    }

    private void setDryerInfoText(@NonNull String BTname) {
        if (BTname.equals("DryerMain"))
            DryerInfoText.setText("主吹風裝置");
        else if (BTname.equals("DryerExtend"))
            DryerInfoText.setText("子吹風裝置");
        else DryerInfoText.setText("不支援的裝置");
    }

    private void setDryerSta(boolean connected) {
        if (connected) {
            DryerStaText.setText("吹風裝置已連線");
            isConnected = true;
            System.out.println("connected");
        }
        if (!connected) {
            DryerStaText.setText("吹風裝置尚未連線");
            isConnected = false;
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

    void UpdateBTMsg() {
        BTWrData.edit()
                .putString("SendMsg", BTSendMsg.toString())
                .apply();
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
                    Log.d(TAG, "ConnBT");

                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BTAddress);
                    MyAppInst.connDevice(device);
                }
            } else if (ID == R.id.PageRefresh) {
                Log.d(TAG, "PageRefresh");
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