package com.ray.mulitfunctionaldryer.util;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.ivbaranov.rxbluetooth.BluetoothConnection;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.ray.mulitfunctionaldryer.component.WaterPieChart;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class MyApp extends Application {
    public static MyApp appInstance;
    private static boolean isConnected;

    public static synchronized MyApp getAppInstance() {
        return appInstance;
    }

    /***Bluetooth***/
    public byte[] buffer = new byte[256];

    private final UUID serialPortUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public StringBuffer BTValTmp = new StringBuffer();

    public synchronized StringBuffer getBTVal() {
        return BTValTmp;
    }

    private BluetoothSocket socket;
    public InputStream inputStream = null;
    public OutputStream outputStream = null;
    private BluetoothAdapter bluetoothAdapter;
    public String DevAddress, DevName;
    private String TAG = "BTSta";
    protected String BTSendMsg;
    char MAIN_DRYER_FIRST_CHAR = 'M';
    char EXTEND_DRYER_FIRST_CHAR = 'E';
    //Flag
    public FlagAddress BTRevSta = new FlagAddress(false);
    public FlagAddress BTRevFlag = new FlagAddress(true);
    public static int isMainDevice = 0;

    /**
     * Sensor value
     */
    public int TempIndex;
    public int WaterIndex;
    //boolean isConnected;


    /**
     * Interface
     */
    public OnConnectedDevice onConnectedDevice;
    public OnUpdateSensorVol onUpdateSensorVol = new OnUpdateSensorVol() {
        @Override
        public void OnWaterChange(int vol) {

        }

    @Override
    public void OnTempChange(int val) {

    }
};

    /**
     * NOTIFY
     **/
    private static final String TEST_NOTIFY_ID = "Dryer_Noti";
    private static final int NOTIFY_REQUEST_ID = 300;


    /**
     * SharedBTValue
     */
    private SharedPreferences BTWrData;
    private SharedPreferences BTRevData;
    /**
     * Strings
     */

    public String TempVal, WaterVal;
    private int[] StrPosition = new int[3];
    private final int BTMsgLen = 7;

    /**
     * RxBluetooth
     **/
    RxBluetooth rxBluetooth = new RxBluetooth(this);
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    AtomicInteger readCnt = new AtomicInteger();

    /**
     * RxUtils
     */
    private RxTimer rxTimer = new RxTimer();


    @Override
    public void onCreate() {
        super.onCreate();

        appInstance = this;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BTWrData = getSharedPreferences("BTMsg", MODE_PRIVATE);
        BTSendMsg = BTWrData.getString("SendMsg", "null");
        BTRevData = getSharedPreferences("BTRev", MODE_PRIVATE);
    }

    /**
     * BlueTooth
     **/

    public void disconnect(Button BTBut) {
        if (socket == null) return;

        try {
            socket.close();
            socket = null;
            inputStream = null;
            outputStream = null;
            BTBut.setText("未連線");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public static boolean getConnected() {
        return isConnected;
    }

    public static void isDisconnected() {
        isConnected = false;
    }

    public void connDevice(BluetoothDevice device) {
        AtomicBoolean Sta = new AtomicBoolean(false);
        compositeDisposable.add(rxBluetooth.connectAsClient(device, serialPortUUID)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        bluetoothSocket -> {
                            // Connected to bluetooth device, do anything with the socket
                            System.out.println("conned");
                            //System.out.println( BTRevSta.Flag);
                            socket = bluetoothSocket;
                            ReadBT();
                            //SubStreamEvent();
                            //sub();
                            isConnected = true;

                            Sta.set(true);
                            try {
                                onConnectedDevice.OnConnected(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }, throwable -> {
                            // On error
                            isConnected = false;
                            System.out.println("error");
                            Sta.set(false);
                            try {
                                onConnectedDevice.OnConnected(false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //System.out.println(ConnAct.getDevice().getName());
                            //System.out.println(ConnAct.getDevice().getAddress());
                        }));
    }

    protected void SubStreamEvent() {
        rxTimer.interval(1000, number -> {
            sub();
        });
    }


    public void WriteBT(String Msg) throws Exception {
        BluetoothConnection blueConn = new BluetoothConnection(socket);
        blueConn.send(Msg); // String
        System.out.println("Now Send:" + Msg);
    }

    private void ReadBT() throws Exception {
        BluetoothConnection bluetoothConnection = new BluetoothConnection(socket);
        compositeDisposable.add(bluetoothConnection.observeByteStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(aByte -> {
                    SavByte(readCnt.get(), aByte);
                    SaveVal(BTValTmp, readCnt.get());
                    readCnt.getAndIncrement();
                    StrProcess(BTValTmp);

                    //BTRevSta.Flag = true;
                    // This will be called every single byte received
                    //System.out.print("Recv byte:");
                    //System.out.println(aByte);
                    //BTRevFlag.Flag = true;
                    //SavByte(aByte);
                    //System.out.println(Arrays.toString(buffer));
                    //System.out.println(buffer[buffer.length-1]);
                }, throwable -> {
                    BTRevSta.Flag = false;
                    throwable.printStackTrace();
                    // Error occured
                    System.out.println("Recv byte Error");
                }));
        BTRevSta.Flag = false;
    }

    protected void SavByte(int count, byte BTByte) {
        if (BTByte < 90)
            buffer[count] = BTByte;
    }

    public void SaveVal(StringBuffer StrBufTmp, int count) {
        if (buffer == null) return;
        String a = new String(buffer, 0, count + 1);

        if (a.charAt(0) != MAIN_DRYER_FIRST_CHAR) {
            System.out.println(a);
            System.out.println("not m return");
            return;
        }
        StrBufTmp.replace(0, count + 1, a);
        //System.out.println(BTValTmp.toString());
    }

    public void StrProcess(StringBuffer strBuffer) {
        int b = 0;

        if (BTValTmp.length() == 0) return;
        if (strBuffer.toString().charAt(0) == MAIN_DRYER_FIRST_CHAR) {
            //System.out.println("first");
            for (int i = 0; i < strBuffer.length(); i++) {
                //System.out.println(i);
                if (strBuffer.toString().getBytes()[i] > 57) {
                    StrPosition[b] = i;
                    if (b != StrPosition.length - 1) b++;
                }
            }
            if (strBuffer.toString().charAt(StrPosition[1]) == 'O') {

                TempVal = strBuffer.toString().substring(StrPosition[0] + 1, StrPosition[0] + 3).trim();
                WaterVal = strBuffer.toString().substring(StrPosition[0] + 3, StrPosition[1]).trim();
                Log.d("Temp", TempVal);
                Log.d("Water", WaterVal);
                if (TempVal.length()!=0 && WaterVal.length()!=0) {
                   TempIndex = Integer.parseInt(TempVal);
                   WaterIndex = Integer.parseInt(WaterVal);
                    //onUpdateSensorVol.OnTempChange(TempIndex);
                    //onUpdateSensorVol.OnWaterChange(WaterIndex);
                    //onUpdateSensorVol.OnTempChange(123);
                }

                //strBuffer.delete(0, strBuffer.length());
                ResetBuffer();
            } else if (strBuffer.toString().charAt(StrPosition[1]) == 'T') {

                switch (strBuffer.toString().charAt(StrPosition[2])) {
                    case 'N':
                        Log.d("BT", "normal end");
                        break;
                    case 'T':
                        Log.d("BT", "temp high end");
                        break;
                    case 'C':
                        Log.d("BT", "cancel end");
                        break;
                }
                ResetBuffer();
            }

        } else if (BTValTmp.toString().charAt(0) == EXTEND_DRYER_FIRST_CHAR) {

        }
    }

    private void ResetBuffer() {
        buffer = new byte[256];
        readCnt = new AtomicInteger();
        BTValTmp.delete(0, BTValTmp.length());
        BTRevFlag.Flag = true;
        StrPosition = new int[3];
    }

    private void SharedBTValue() {
        int TempIntVal;
        if (TempVal == null || TempVal.equals("")) TempIntVal = 0;
        else TempIntVal = Integer.parseInt(TempVal);
        int WaterIntVal;
        if (WaterVal == null || WaterVal.equals("")) WaterIntVal = 0;
        else WaterIntVal = Integer.parseInt(WaterVal);

        BTRevData.edit()
                .putInt("Water", WaterIntVal)
                .putInt("Temp", TempIntVal)
                .apply();

        //System.out.println("Shared BTval!");
    }

    /**
     * 創建觀察者，觀察藍芽字串
     */
    ObservableOnSubscribe<String> observableOnSubscribe = new ObservableOnSubscribe<String>() {
        @Override
        public void subscribe(ObservableEmitter<String> emitter) {
            //System.out.println("已經訂閱：subscribe，获取发射器");
            // if (RxLocation != null)
            //    emitter.onNext(RxLocation);
            //
            if (BTRevFlag.Flag) {

                if (BTSendMsg == null) return;
                BTSendMsg = BTWrData.getString("SendMsg", "null");
                if (BTSendMsg.equals("null")) {
                    //System.out.println("Msg null");
                    //return;
                }
                emitter.onNext("$NND00N000O");
                //emitter.onNext(BTSendMsg);
            }

            //System.out.println("信號發射：onComplete");
        }
    };

    /**
     * 创建被观察者，并带上被观察者的订阅
     */
    Observable<String> observable = Observable.create(observableOnSubscribe);

    final Disposable[] disposable = new Disposable[1];

    io.reactivex.Observer<String> observer = new Observer<String>() {
        @Override
        public void onSubscribe(Disposable d) {
            disposable[0] = d;
            //System.out.println("已经订阅：onSubscribe，获取解除器");
        }

        @Override
        public void onNext(String string) {
            try {
                //if (readCnt.get() >= 9) {
                System.out.println("next");
                WriteBT(string);
                buffer = new byte[256];
                readCnt = new AtomicInteger();

                BTValTmp.delete(0, BTValTmp.length());
                SharedBTValue();
                //}
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onError(Throwable e) {
            // System.out.println("信号接收：onError " + e.getMessage());
            cancel();
        }

        @Override
        public void onComplete() {
            //System.out.println("信号接收：onComplete");
        }
    };

    public void sub() {
        //System.out.println("開始訂閱：subscribe");
        observable.subscribe(observer);
    }

    public void cancel() {
        System.out.println("取消訂閱：unsubscribe");
        if (disposable[0] != null)
            disposable[0].dispose();
    }

    public int getTempIndex(){
        return TempIndex;
    }
    public int getWaterIndex(){
        return WaterIndex;
    }
    public static int getDeviceIndex() {
        return isMainDevice;
    }

    public static void setDeviceIndex(int i){
        isMainDevice = i;
    }

    public interface OnUpdateSensorVol {
        void OnWaterChange(int vol);

        void OnTempChange(int val);
    }

    public interface OnConnectedDevice {
        void OnConnected(boolean ans) throws Exception;
    }
}
