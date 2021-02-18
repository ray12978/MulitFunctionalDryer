package com.ray.mulitfunctionaldryer.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ray.mulitfunctionaldryer.R;
import com.ray.mulitfunctionaldryer.component.BottomNavigation;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public StringBuffer BTSendMsg = new StringBuffer("$NND00N000O"); //[0]StartBit[1]Lock{L,F,N},[2]SpeedTen,[3]SpeedUnit,[4]SpeedConfirm,[5]Laser{T,J,N},[6]Buzzer{E,N},[7]CloudMode{Y,N}
    private final int BT_MSG_LEN = 11;
    private PieChart pieChart;
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
        InitChart();
    }

    private void Initialize() {
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

    void InitChart() {
        pieChart = (PieChart) findViewById(R.id.piechart);

        pieChart.setRotationEnabled(false);     // 可以手动旋转
        pieChart.setUsePercentValues(true);     //显示成百分比
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);

        pieChart.setDragDecelerationFrictionCoef(0.99f);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setEntryLabelTextSize(15f);

        pieChart.animateY(1000, Easing.EaseInOutCubic);     //動畫
        setPieChart(60.5f);

        pieChart.setCenterText("現在水量");
        pieChart.setCenterTextSize(30f);
    }

    private void setPieChart(float val) {
        //大小
        ArrayList<PieEntry> sizes = new ArrayList<>();
        sizes.add(new PieEntry(val, "%"));
        sizes.add(new PieEntry(100-val, "%",R.drawable.drawable_home));

        PieDataSet pieDataSet = new PieDataSet(sizes, "");

        pieDataSet.setSliceSpace(0f);
        pieDataSet.setSelectionShift(5f);
        pieDataSet.setDrawIcons(true);
        pieDataSet.setDrawValues(true);


        //颜色
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#87CEEB"));
        colors.add(Color.parseColor("#808A87"));
        pieDataSet.setColors(colors);

        PieData data = new PieData((pieDataSet));
        data.setValueTextSize(25f);
        data.setValueTextColor(Color.BLACK);
        pieChart.setData(data);
        pieChart.invalidate();
    }
}