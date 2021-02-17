package com.ray.mulitfunctionaldryer.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.util.BottomNavigation;

public class MainActivity extends AppCompatActivity {
    public StringBuffer BTSendMsg = new StringBuffer("$NND00N000O"); //[0]StartBit[1]Lock{L,F,N},[2]SpeedTen,[3]SpeedUnit,[4]SpeedConfirm,[5]Laser{T,J,N},[6]Buzzer{E,N},[7]CloudMode{Y,N}
    private int BT_MSG_LEN = 11;

    /**
     * SharedPreferences
     **/
    private SharedPreferences BTWrData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavInit();
        Initialize();
    }

    private void Initialize(){
        BTWrData = getSharedPreferences("BTMsg", MODE_PRIVATE);

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
}