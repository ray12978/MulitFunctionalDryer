package com.ray.mulitfunctionaldryer.component;

import android.graphics.Color;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.MPPointF;
import com.ray.mulitfunctionaldryer.R;

import java.util.ArrayList;

public class WaterPieChart {
    PieChart pieChart;

    public WaterPieChart(PieChart pieChart) {
        this.pieChart = pieChart;
    }

    public void InitChart() {
        //pieChart = (com.github.mikephil.charting.charts.PieChart) findViewById(R.id.piechart);

        pieChart.setRotationEnabled(false);     // 可以手动旋转
        pieChart.setUsePercentValues(true);     //显示成百分比
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);

        pieChart.setDragDecelerationFrictionCoef(0.99f);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        //pieChart.setTransparentCircleRadius(61f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setEntryLabelTextSize(15f);

        pieChart.animateY(1000, Easing.EaseInOutCubic);     //動畫
        //setPieChartValue(60.5f);

        pieChart.setCenterTextSize(35f);
        pieChart.setCenterTextColor(0xFF2EA9DF);
    }

    public void setPieChartValue(float val) {
        //大小
        ArrayList<PieEntry> sizes = new ArrayList<>();
        sizes.add(new PieEntry(val));
        sizes.add(new PieEntry(100-val));

        PieDataSet pieDataSet = new PieDataSet(sizes, "");

        pieDataSet.setSliceSpace(0f);
        pieDataSet.setSelectionShift(5f);

        pieDataSet.setVisible(true);




        //颜色
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#2EA9DF")); //value color "#87CEEB"
        colors.add(Color.parseColor("#192E5B")); //empty color "#808A87"
        pieDataSet.setColors(colors);

        PieData data = new PieData((pieDataSet));
        data.setValueTextSize(0f);
        data.setValueTextColor(Color.WHITE);
        pieChart.setData(data);
        pieChart.invalidate();
        pieChart.setCenterText("現在水量\r\n"+ val + "%");
    }
}
