package com.ray.mulitfunctionaldryer.util;

import android.content.Context;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.view.ConsoleActivity;
import com.ray.mulitfunctionaldryer.view.MainActivity;
import com.ray.mulitfunctionaldryer.view.SelectBTActivity;

public final class BottomNavigation {
    Context context;
    BottomNavigationView bottomNav;
    int index = 0;
    //MyApplication myApp = new MyApplication();

    public BottomNavigation(Context context, BottomNavigationView nav, int index){
        this.context = context;
        this.bottomNav = nav;
        this.index = index;
    }
    public void init(){
        bottomNav.getMenu().getItem(index).setChecked(true);

        bottomNav.setOnNavigationItemSelectedListener((item) -> {
            switch (item.getItemId()) {
                case R.id.nav1:
                    Intent intent = new Intent(context, MainActivity.class);
                    context.startActivity(intent);
                    break;
                case R.id.nav2:
                    Intent intent2 = new Intent(context, ConsoleActivity.class);
                    context.startActivity(intent2);
                    break;
                case R.id.nav3:
                    Intent intent3 = new Intent(context, SelectBTActivity.class);
                    context.startActivity(intent3);
                    break;
            }
            return true;
        });
    }
}
