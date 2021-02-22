package com.ray.mulitfunctionaldryer.util;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.ivbaranov.rxbluetooth.BluetoothConnection;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;

import androidx.annotation.NonNull;

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
    char BT_FIRST_CHAR = 'M';
    //Flag
    public FlagAddress BTRevSta = new FlagAddress(false);
    public FlagAddress BTRevFlag = new FlagAddress(false);

    public OnConnectedDevice onConnectedDevice;
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
    private int[] StrPosition = new int[2];
    private final int BTMsgLen = 7;

    /**
     * RxBluetooth
     **/
    RxBluetooth rxBluetooth = new RxBluetooth(this);
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    AtomicInteger readCnt = new AtomicInteger();

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

    public interface OnConnectedDevice {
        void OnConnected(boolean ans) throws Exception;
    }

    public boolean connDevice(BluetoothDevice device) {
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
                            //AutoWriteBT();
                            Sta.set(true);
                            try {
                                onConnectedDevice.OnConnected(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }, throwable -> {
                            // On error
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
        return Sta.get();
    }


    protected void WriteBT(String Msg) throws Exception {
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
                    //buffer[i] = aByte;
                    SavByte(readCnt.get(), aByte);
                    SaveVal(BTValTmp, readCnt.get());
                    readCnt.getAndIncrement();
                    BTRevSta.Flag = true;
                    // This will be called every single byte received
                    //System.out.print("Recv byte:");
                    //System.out.println(aByte);
                    BTRevFlag.Flag = true;
                    //SavByte(aByte);
                    //System.out.println(Arrays.toString(buffer));
                    //System.out.println(buffer[buffer.length-1]);
                }, throwable -> {
                    BTRevSta.Flag = false;
                    // Error occured
                    System.out.println("Recv byte Error");
                }));
        BTRevSta.Flag = false;
    }

    protected void SavByte(int count, byte BTByte) {
        buffer[count] = BTByte;
    }

    public void SaveVal(@NonNull StringBuffer StrBufTmp, int count) {
        if (buffer == null) return;

        String a = new String(buffer, 0, count + 1);
        if (a.charAt(0) != BT_FIRST_CHAR) return;
        StrBufTmp.replace(0, count + 1, a);
    }

    public void StrProcess() {
        int b = 0;

        if (BTValTmp.length() == 0) return;
        if (BTValTmp.toString().charAt(0) == BT_FIRST_CHAR) {
            for (int i = 0; i < BTValTmp.length(); i++) {
                if (BTValTmp.toString().getBytes()[i] > 57) {
                    StrPosition[b] = i;
                    if (b != StrPosition.length - 1) b++;
                }
            }
            //System.out.println( BTValTmp.toString()+','+StrPosition[1]+','+StrPosition[2]);
            TempVal = BTValTmp.toString().substring(StrPosition[0] + 1, StrPosition[0] + 3).trim();
            WaterVal = BTValTmp.toString().substring(StrPosition[0] + 3, StrPosition[1] - 1).trim();
        }
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
                    return;
                }

                emitter.onNext(BTSendMsg);
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
                if (readCnt.get() >= 9) {
                    WriteBT(string);
                    buffer = new byte[256];
                    readCnt = new AtomicInteger();
                    StrProcess();
                    BTValTmp.delete(0, BTValTmp.length());
                    SharedBTValue();
                }
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

}
