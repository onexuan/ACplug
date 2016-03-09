package digimagus.csrmesh.acplug.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Collections;
import java.util.List;

import digimagus.csrmesh.acplug.R;

/**
 *
 *
 */
public class SummaryChart {

    private final static String TAG = "SummaryChart";

    private GraphicalView view;
    private XYMultipleSeriesDataset dataset;
    private XYMultipleSeriesRenderer renderer;

    private Context context;
    private String title;

    public GraphicalView init(Context context, List<String> x, List<Double> y) {
        refreshUI(x, y, title);
        this.context = context;
        if (view == null) {
            view = ChartFactory.getCubeLineChartView(this.context, dataset, renderer, 0.2f);
        }
        return view;
    }

    public void refreshUI(List<String> x, List<Double> y, String title) {
        this.title = title;
        Double yMax = Collections.max(y);
        yMax = (yMax == 0 ? 1 : yMax);
        Double yMin = Collections.min(y);
        dataset = buildDataset(y);
        renderer = buildRenderer();
       /* if(title!=null){
            renderer.setChartTitle(context.getString(R.string.battery_chart,title));
        }*/
        renderer.setShowGrid(true);
        renderer.setShowLegend(false);
        renderer.setPanEnabled(true, false);
        renderer.setPanLimits(new double[]{0, x.size(), 0, yMax});
        renderer.setApplyBackgroundColor(true);//必须设置为true，颜色值才生效
        renderer.setBackgroundColor(Color.WHITE);//设置表格背景色
        renderer.setMarginsColor(Color.WHITE);//设置周边背景色
        renderer.setZoomEnabled(false, false);
        renderer.setXLabelsAngle(-25); // 设置 X 轴标签倾斜角度 (clockwise degree)
        renderer.setXLabels(0); // 设置 X 轴不显示数字（改用我们手动添加的文字标签）
        renderer.setYLabels(5);
        for (int i = 0; i < x.size(); i++) {
            if (x.size() >= 180 && i % 10 == 0) {
                renderer.addXTextLabel(i, x.get(i));
            } else if (x.size() <= 180 && x.size() >= 90 && i % 5 == 0) {
                renderer.addXTextLabel(i, x.get(i));
            } else if (x.size() <= 90 && x.size() >= 30 && i % 2 == 0) {
                renderer.addXTextLabel(i, x.get(i));
            } else if (x.size() <= 30) {
                renderer.addXTextLabel(i, x.get(i));
            }
        }
        setChartSettings(x.size(), yMin, yMax, Color.GRAY);
    }

    protected XYMultipleSeriesDataset buildDataset(List<Double> yValues) {
        if (dataset == null) {
            dataset = new XYMultipleSeriesDataset();
        } else {
            if (dataset.getSeriesCount() > 1) {
                dataset.removeSeries(1);
            }
        }
        XYValueSeries series = new XYValueSeries("");    //根据每条线的名称创建
        for (int k = 0; k < yValues.size(); k++) {        //每条线里有几个点
            series.add(k, yValues.get(k));
        }
        dataset.addSeries(series);
        return dataset;
    }

    protected XYMultipleSeriesRenderer buildRenderer() {
        if (renderer == null) {
            renderer = new XYMultipleSeriesRenderer();
        } else {
            renderer.clearXTextLabels();
            renderer.clearYTextLabels();
        }
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(Color.GRAY);
        r.setPointStyle(PointStyle.POINT);
        r.setLineWidth(1.8f);
        renderer.addSeriesRenderer(r);
        renderer.setXLabelsAlign(Paint.Align.CENTER);
        renderer.setChartTitleTextSize(20f);/* 设置表格标题字体大小 */
        renderer.setLabelsTextSize(20f);
        renderer.setAxisTitleTextSize(20f);
        return renderer;
    }

    protected void setChartSettings(double xMax, double yMin, double yMax, int axesColor) {
        renderer.setXAxisMin(0);
        renderer.setXAxisMax(xMax);
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
        renderer.setAxesColor(axesColor);
        renderer.setLabelsColor(axesColor);
    }
}
