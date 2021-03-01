package com.ray.mulitfunctionaldryer.util;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.ivbaranov.rxbluetooth.BluetoothConnection;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.component.TimePickerDialog;
import com.ray.mulitfunctionaldryer.component.WaterPieChart;
import com.ray.mulitfunctionaldryer.view.ConsoleActivity;
import com.ray.mulitfunctionaldryer.view.MainActivity;

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
    public static boolean isConnected;

    public static synchronized MyApp getAppInstance() {
        return appInstance;
    }

    /***Bluetooth***/
    public byte[] buffer = new byte[256];

    private final UUID serialPortUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public StringBuffer BTValTmp = new StringBuffer();

    private BluetoothSocket socket;
    public InputStream inputStream = null;
    public OutputStream outputStream = null;

    private String TAG = "BTSta";
    char MAIN_DRYER_FIRST_CHAR = 'M';
    char EXTEND_DRYER_FIRST_CHAR = 'E';
    //Flag
    public FlagAddress BTRevSta = new FlagAddress(false);
    public FlagAddress BTRevFlag = new FlagAddress(true);
    public static int isMainDevice = 0;
    public static boolean WaterBeeped = false;
    public static boolean TempBeeped = false;
    /**
     * Sensor value
     */
    public int TempIndex;
    public int WaterIndex;


    /**
     * Interface
     */
    public OnConnectedDevice onConnectedDevice;

    /**
     * NOTIFY
     **/
    private static final String TEST_NOTIFY_ID = "Dryer_Noti";
    private static final int NOTIFY_REQUEST_ID = 300;

    /**
     * Strings
     **/

    public String TempVal, WaterVal;
    private int[] StrPosition = new int[3];

    /**
     * RxBluetooth
     **/
    RxBluetooth rxBluetooth = new RxBluetooth(this);
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    AtomicInteger readCnt = new AtomicInteger();

    /**
     * Timer
     **/
    static int[] TimeSaver = new int[4];
    static String TimerString = "";
    final RxTimer CountDownTimer = new RxTimer();
    boolean isStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();

        appInstance = this;

    }

    /**
     * BlueTooth
     **/

    public void disconnect() {
        if (socket == null) return;

        try {
            socket.close();
            socket = null;
            inputStream = null;
            outputStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void StartCount() {
        if(!isStarted){
            CountDownTimer.interval(1000, number -> {
                TimerCountdown();
            });
            isStarted = true;
        }

    }

    public void StopCount(){
        isStarted = false;
        CountDownTimer.cancel();
    }

    @SuppressLint("DefaultLocale")
    private void TimerCountdown() {
        if (TimeSaver[2] > 0) TimeSaver[2] -= 1;
        else if (TimeSaver[1] > 0) {
            TimeSaver[2] = 59;
            TimeSaver[1] -= 1;
        } else if (TimeSaver[0] > 0) {
            TimeSaver[0] -= 1;
            TimeSaver[1] = 59;
        }
        String hours = String.format("0%d:", TimeSaver[0]);
        String minutes = TimeSaver[1] >= 10 ? String.format("%d:", TimeSaver[1]) : String.format("0%d:", TimeSaver[1]);
        String seconds = TimeSaver[2] >= 10 ? String.format("%d", TimeSaver[2]) : String.format("0%d", TimeSaver[2]);
        String times = "";

        times = times.concat(hours);
        times = times.concat(minutes);
        times = times.concat(seconds);

        TimerString = times;
    }

    public static void setTimeString(String s) {
        TimerString = s;
    }

    public static String getTimeString() {
        return TimerString;
    }

    public static void setTimeSaver(int[] times) {
        TimeSaver = times;
    }

    public static int[] getTimeSaver() {
        return TimeSaver;
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
                            System.out.println("conned");
                            socket = bluetoothSocket;
                            ReadBT();
                            isConnected = true;
                            Sta.set(true);
                            try {
                                onConnectedDevice.OnConnected(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }, throwable -> {
                            isConnected = false;
                            System.out.println("error");
                            Sta.set(false);
                            try {
                                onConnectedDevice.OnConnected(false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }));
    }

    public void WriteBT(String Msg) throws Exception {
        BluetoothConnection blueConn = new BluetoothConnection(socket);
        blueConn.send(Msg);
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
                }, throwable -> {
                    BTRevSta.Flag = false;
                    throwable.printStackTrace();
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
            return;
        }
        StrBufTmp.replace(0, count + 1, a);
    }

    public void StrProcess(StringBuffer strBuffer) {
        int b = 0;

        if (BTValTmp.length() == 0) return;
        System.out.println(strBuffer);
        if (strBuffer.toString().charAt(0) == MAIN_DRYER_FIRST_CHAR) {
            for (int i = 0; i < strBuffer.length(); i++) {
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
                if (TempVal.length() != 0 && WaterVal.length() != 0) {
                    TempIndex = Integer.parseInt(TempVal);
                    WaterIndex = Integer.parseInt(WaterVal);
                }
                ResetBuffer();
            } else if (strBuffer.toString().charAt(StrPosition[1]) == 'T' && strBuffer.length() > 2) {
                switch (strBuffer.toString().charAt(StrPosition[2])) {
                    case 'N':
                        Log.d("BT", "normal end");
                        TimeModeEndNotify(getString(R.string.main_time_mode_notify_title), getString(R.string.end_reason_normal));
                        break;
                    case 'T':
                        Log.d("BT", "temp high end");
                        TimeModeEndNotify(getString(R.string.main_time_mode_notify_title), getString(R.string.end_reason_temp));
                        break;
                    case 'C':
                        Log.d("BT", "cancel end");
                        TimeModeEndNotify(getString(R.string.main_time_mode_notify_title), getString(R.string.end_reason_cancel));
                        break;
                }
                ConsoleActivity.isTiming = false;
                StopCount();
                ResetBuffer();
            }
        } else if (BTValTmp.toString().charAt(0) == EXTEND_DRYER_FIRST_CHAR) {
            for (int i = 0; i < strBuffer.length(); i++) {
                if (strBuffer.toString().getBytes()[i] > 57) {
                    StrPosition[b] = i;
                    if (b != StrPosition.length - 1) b++;
                }
            }
            if (strBuffer.toString().charAt(StrPosition[1]) == 'T' && strBuffer.length() > 2) {
                switch (strBuffer.toString().charAt(StrPosition[2])) {
                    case 'N':
                        Log.d("BT", "normal end");
                        TimeModeEndNotify(getString(R.string.extend_time_mode_notify_title), getString(R.string.end_reason_normal));
                        break;
                    case 'C':
                        Log.d("BT", "cancel end");
                        TimeModeEndNotify(getString(R.string.extend_time_mode_notify_title), getString(R.string.end_reason_cancel));
                        break;
                }
                StopCount();
                ConsoleActivity.isTiming = false;
                ResetBuffer();
            }
        }
    }

    private void ResetBuffer() {
        buffer = new byte[256];
        readCnt = new AtomicInteger();
        BTValTmp.delete(0, BTValTmp.length());
        BTRevFlag.Flag = true;
        StrPosition = new int[3];
    }

    public void WaterEmptyNotify() {
        Log.d(TAG, "WaterEmptyNotify: ");
        try {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("noti_id", NOTIFY_REQUEST_ID);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    NOTIFY_REQUEST_ID,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder builder = new Notification.Builder(this)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(getString(R.string.water_notify_title))
                    .setContentText(getString(R.string.water_notify_text))
                    .setShowWhen(true)
                    .setLights(0xff00ff00, 300, 1000)
                    .setSmallIcon(R.drawable.drawable_water_drop)
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationChannel channel;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(TEST_NOTIFY_ID
                        , "Water Empty"
                        , NotificationManager.IMPORTANCE_HIGH);
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.shouldShowLights();
                channel.setLightColor(Color.GREEN);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                builder.setChannelId(TEST_NOTIFY_ID);

                manager.createNotificationChannel(channel);
            } else {
                builder.setDefaults(Notification.DEFAULT_ALL)
                        .setVisibility(Notification.VISIBILITY_PUBLIC);
            }
            int testNotifyId = 12;
            manager.notify(testNotifyId,
                    builder.build());
        } catch (Exception e) {

        }
    }

    public void TempHighNotify() {
        Log.d(TAG, "TempHighNotify: ");
        try {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("noti_id", NOTIFY_REQUEST_ID);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    NOTIFY_REQUEST_ID,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder builder = new Notification.Builder(this)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(getString(R.string.temp_notify_title))
                    .setContentText(getString(R.string.temp_notify_text))
                    .setShowWhen(true)
                    .setLights(0xff00ff00, 300, 1000)
                    .setSmallIcon(R.drawable.drawable_temp)
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationChannel channel;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(TEST_NOTIFY_ID
                        , "Water Empty"
                        , NotificationManager.IMPORTANCE_HIGH);
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.shouldShowLights();
                channel.setLightColor(Color.GREEN);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                builder.setChannelId(TEST_NOTIFY_ID);

                manager.createNotificationChannel(channel);
            } else {
                builder.setDefaults(Notification.DEFAULT_ALL)
                        .setVisibility(Notification.VISIBILITY_PUBLIC);
            }
            int testNotifyId = 13;
            manager.notify(testNotifyId,
                    builder.build());
        } catch (Exception e) {

        }
    }

    public void TimeModeEndNotify(String title, String text) {
        Log.d(TAG, "TimeModeEndNotify: ");
        try {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("noti_id", NOTIFY_REQUEST_ID);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                    NOTIFY_REQUEST_ID,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder builder = new Notification.Builder(this)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(title)
                    .setContentText(text)
                    .setShowWhen(true)
                    .setLights(0xff00ff00, 300, 1000)
                    .setSmallIcon(R.drawable.drawable_timer_white)
                    .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationChannel channel;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(TEST_NOTIFY_ID
                        , "Water Empty"
                        , NotificationManager.IMPORTANCE_HIGH);
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.shouldShowLights();
                channel.setLightColor(Color.GREEN);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                builder.setChannelId(TEST_NOTIFY_ID);

                manager.createNotificationChannel(channel);
            } else {
                builder.setDefaults(Notification.DEFAULT_ALL)
                        .setVisibility(Notification.VISIBILITY_PUBLIC);
            }
            int testNotifyId = 14;
            manager.notify(testNotifyId,
                    builder.build());
        } catch (Exception e) {

        }
    }


    public int getTempIndex() {
        return TempIndex;
    }

    public int getWaterIndex() {
        return WaterIndex;
    }

    public static int getDeviceIndex() {
        return isMainDevice;
    }

    public static void setDeviceIndex(int i) {
        isMainDevice = i;
    }

    public interface OnConnectedDevice {
        void OnConnected(boolean ans) throws Exception;
    }
}
