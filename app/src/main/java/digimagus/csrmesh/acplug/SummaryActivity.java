package digimagus.csrmesh.acplug;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.digimagus.aclibrary.HTTPManagementAPI;

import org.achartengine.GraphicalView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import digimagus.csrmesh.acplug.util.SummaryChart;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.view.MyPagerAdapter;
import digimagus.csrmesh.view.NoScrollViewPager;
import digimagus.csrmesh.view.ViewPagerScroller;


public class SummaryActivity extends BaseActivity implements View.OnClickListener {
    private final static String TAG = "SummaryActivity";
    private ImageView back;
    private TextView previous, next, history;
    private TextView name, online, power;
    private Bundle bundle;

    private List<Double> usageList = new ArrayList<>();//电量使用历史
    private List<Double> costList = new ArrayList<>();//消费历史

    private List<String> xRawDatas = new ArrayList<>();
    private NoScrollViewPager viewpage;
    private List<View> viewList = new ArrayList<>();
    private TextView hr, day, wk, mth, mth3, mth6, yr1, yr2;
    private boolean summaryType = true;
    private Calendar calendar = null;
    private HTTPManagementAPI httpAPI;
    private String uuid, wislink_uuid, wislink_token;
    private LinearLayout loadata, summary;
    private DeviceInfo deviceInfo;
    private GraphicalView view1, view2;
    private SummaryChart summaryChart1, summaryChart2;
    List<TextView> tabMenu = new ArrayList<>();
    /**
     * 格式化 时间 ISO 8601
     */
    private SimpleDateFormat iso8601_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ms'Z'");
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private DecimalFormat decimalFormat = new DecimalFormat("00");
    private ExecutorService pool = Executors.newFixedThreadPool(1);

    public static String dynamicFormatTime(String format, Calendar calendar) {
        return new SimpleDateFormat(format).format(calendar.getTime());
    }

    private Map<String, ChartBattery> Chart_HR = new HashMap<>();
    private Map<String, ChartBattery> Chart_DAY = new HashMap<>();
    private Map<String, ChartBattery> Chart_WK = new HashMap<>();
    private Map<String, ChartBattery> Chart_MTH = new HashMap<>();
    private Map<String, ChartBattery> Chart_3MTH = new HashMap<>();
    private Map<String, ChartBattery> Chart_6MTH = new HashMap<>();
    private Map<String, ChartBattery> Chart_1YR = new HashMap<>();
    private Map<String, ChartBattery> Chart_2YR = new HashMap<>();

    @Override
    protected void handler(Message msg) {
        //Log.e(TAG, "udp: " + msg.obj);
        if (msg.what == NOTYFY_UI_CHANGE) {
            String serial = String.valueOf(msg.obj);
            if (bundle.getString("serial").equals(serial)) {
                DeviceInfo device = devices.get(serial);
                power.setText(String.valueOf(device.power + " w"));
                online.setText(device.online ? (device.state == 1 ? "on" : "off") : "online");
            }
        }
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_summary);
        bundle = getIntent().getBundleExtra("bundle");
        calendar = Calendar.getInstance();
        httpAPI = HTTPManagementAPI.getInstance();
        uuid = bundle.getString("uuid");
        wislink_uuid = phoneInfo.getUuid();
        wislink_token = phoneInfo.getToken();
        deviceInfo = devices.get(bundle.getString("serial"));
        initFindViewById();
    }

    private void initFindViewById() {
        summary = (LinearLayout) findViewById(R.id.summary);
        back = (ImageView) findViewById(R.id.back);
        name = (TextView) findViewById(R.id.name);
        history = (TextView) findViewById(R.id.history);

        viewpage = (NoScrollViewPager) findViewById(R.id.viewpage);
        viewpage.setNoScroll(true);

        List<Double> y = new ArrayList();
        y.add(0.0);
        y.add(0.0);
        y.add(0.0);
        y.add(0.0);
        y.add(0.0);
        y.add(0.0);
        List<String> x = new ArrayList();
        x.add("1");
        x.add("2");
        x.add("3");
        x.add("4");
        x.add("5");
        x.add("6");
        summaryChart1 = new SummaryChart();
        summaryChart2 = new SummaryChart();
        view1 = summaryChart1.init(this, x, y);
        view2 = summaryChart2.init(this, x, y);
        viewList.add(view1);
        viewList.add(view2);

        viewpage.setAdapter(new MyPagerAdapter(viewList));
        previous = (TextView) findViewById(R.id.previous);
        next = (TextView) findViewById(R.id.next);
        loadata = (LinearLayout) findViewById(R.id.loadata);

        hr = (TextView) findViewById(R.id.hr);
        day = (TextView) findViewById(R.id.day);
        wk = (TextView) findViewById(R.id.wk);
        mth = (TextView) findViewById(R.id.mth);
        mth3 = (TextView) findViewById(R.id.mth3);
        mth6 = (TextView) findViewById(R.id.mth6);
        yr1 = (TextView) findViewById(R.id.yr1);
        yr2 = (TextView) findViewById(R.id.yr2);
        online = (TextView) findViewById(R.id.online);
        power = (TextView) findViewById(R.id.power);

        tabMenu.add(hr);
        tabMenu.add(day);
        tabMenu.add(wk);
        tabMenu.add(mth);
        tabMenu.add(mth3);
        tabMenu.add(mth6);
        tabMenu.add(yr1);
        tabMenu.add(yr2);

        hr.setOnClickListener(this);
        day.setOnClickListener(this);
        wk.setOnClickListener(this);
        mth.setOnClickListener(this);
        mth3.setOnClickListener(this);
        mth6.setOnClickListener(this);
        yr1.setOnClickListener(this);
        yr2.setOnClickListener(this);

        back.setOnClickListener(this);
        next.setOnClickListener(this);
        previous.setOnClickListener(this);

        viewpage.setCurrentItem(0, true);
        name.setText(deviceInfo.getName());
        online.setText(bundle.getString("online"));
        power.setText(String.valueOf(devices.get(bundle.getString("serial")).power + " w"));
    }

    /**
     * 请求失败
     * 1 Hr
     * 2 Day
     * 3 wk
     * 4 mth
     * 5 3mth
     * 6 6mth
     * 7 1yr
     * 8 2yr
     */
    public final static int REQUEST_FAIL = 0x0;
    public final static int REQUEST_HR = 0x01;
    public final static int REQUEST_DAY = 0x02;
    public final static int REQUEST_WK = 0x03;
    public final static int REQUEST_MTH = 0x04;
    public final static int REQUEST_3MTH = 0x05;
    public final static int REQUEST_6MTH = 0x06;
    public final static int REQUEST_1YR = 0x07;
    public final static int REQUEST_2YR = 0x08;

    public static int REQUEST_PARAMETER = REQUEST_FAIL;


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            usageList.clear();
            costList.clear();
            xRawDatas.clear();
            switch (msg.what) {
                case REQUEST_FAIL: {
                    Log.e(TAG, "FAIL : 请求失败");
                    break;
                }
                case REQUEST_HR: {
                    try {
                        if (msg.arg2 != 2) {
                            JSONArray arr = new JSONArray(String.valueOf(msg.obj));
                            double LastData = 0;
                            Calendar lastcalendar = null;
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                Log.e(TAG, "JSONObject   " + obj);
                                Calendar calendar = Calendar.getInstance();
                                String timestamp = String.valueOf(obj.getString("timestamp")).replace('T', ' ');
                                timestamp = timestamp.substring(0, timestamp.indexOf("."));
                                calendar.setTime(format.parse(timestamp));
                                calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + timeZone());//标准时间

                                if (lastcalendar != null) {
                                    double theData = Double.parseDouble(obj.getString("EnergyP")) - LastData;
                                    theData = theData < 0 ? 0 : theData;
                                    ChartBattery battery = new ChartBattery();
                                    StringBuilder sb = new StringBuilder(dynamicFormatTime("HH:mm", calendar));
                                    battery.key = sb.replace(4,5,"0").toString();
                                    battery.setCost(theData * deviceInfo.getPrice());
                                    battery.setUsage(theData);
                                    Chart_HR.put(battery.key, battery);

                                    lastcalendar = calendar;
                                    LastData = theData;
                                    Log.e(TAG, "REQUEST_HR  " + battery.key);
                                } else {
                                    LastData = Double.parseDouble(obj.getString("EnergyP"));
                                    lastcalendar = calendar;
                                }
                            }
                        }
                        for (ChartBattery battery : Chart_HR.values()) {
                            xRawDatas.add(battery.getKey());
                            usageList.add(battery.getUsage());
                            costList.add(battery.getCost());
                        }
                        Collections.sort(xRawDatas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case REQUEST_DAY: {
                    try {
                        if (msg.arg2 != 2) {
                            JSONArray arr = new JSONArray(String.valueOf(msg.obj));
                            double LastData = 0;
                            Calendar lastcalendar = null;
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                Calendar calendar = Calendar.getInstance();
                                String timestamp = String.valueOf(obj.getString("timestamp")).replace('T', ' ');
                                timestamp = timestamp.substring(0, timestamp.indexOf("."));
                                calendar.setTime(format.parse(timestamp));

                                if (lastcalendar != null) {
                                    double theData = Double.parseDouble(obj.getString("EnergyP")) - LastData;
                                    theData = theData < 0 ? 0 : theData;
                                    Chart_DAY.get(decimalFormat.format(calendar.get(Calendar.HOUR_OF_DAY))).setUsage(theData);
                                    Chart_DAY.get(decimalFormat.format(calendar.get(Calendar.HOUR_OF_DAY))).setCost(theData * deviceInfo.getPrice());

                                    lastcalendar = calendar;
                                    LastData = theData;
                                } else {
                                    LastData = Double.parseDouble(obj.getString("EnergyP"));
                                    lastcalendar = calendar;
                                }
                            }
                        }
                        for (int i = 0; i < Chart_DAY.size(); i++) {
                            xRawDatas.add(String.valueOf(i));
                            usageList.add(Chart_DAY.get(String.valueOf(i)).getUsage());
                            costList.add(Chart_DAY.get(String.valueOf(i)).getCost());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case REQUEST_WK: {
                    try {
                        if (msg.arg2 != 2) {
                            JSONArray arr = new JSONArray(String.valueOf(msg.obj));
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                if (i != 0) {
                                    String key = bzTime(obj.getString("timestamp")).substring(8, 10);
                                    double usage = Double.parseDouble(obj.getString("EnergyP")) - Double.parseDouble(arr.getJSONObject(i - 1).getString("EnergyP"));
                                    usage = usage < 0 ? 0 : usage;
                                    Chart_WK.get(key).setUsage(usage);
                                    Chart_WK.get(key).setCost(usage * deviceInfo.getPrice());
                                }
                            }
                        }
                        for (int i = 0; i < XLabel.length; i++) {
                            xRawDatas.add(XLabel[i]);
                            usageList.add(Chart_WK.get(XLabel[i]).getUsage());
                            costList.add(Chart_WK.get(XLabel[i]).getCost());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case REQUEST_MTH: {
                    try {
                        if (msg.arg2 != 2) {
                            JSONArray arr = new JSONArray(String.valueOf(msg.obj));
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                if (i != 0) {
                                    String key = bzTime(obj.getString("timestamp")).substring(8, 10);
                                    double usage = Double.parseDouble(obj.getString("EnergyP")) - Double.parseDouble(arr.getJSONObject(i - 1).getString("EnergyP"));
                                    usage = usage < 0 ? 0 : usage;
                                    Chart_MTH.get(key).setUsage(usage);
                                    Chart_MTH.get(key).setCost(usage * deviceInfo.getPrice());
                                }
                            }
                        }
                        for (int i = 0; i < XLabel.length; i++) {
                            xRawDatas.add(XLabel[i]);
                            usageList.add(Chart_MTH.get(XLabel[i]).getUsage());
                            costList.add(Chart_MTH.get(XLabel[i]).getCost());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case REQUEST_3MTH: {
                    try {
                        if (msg.arg2 != 2) {
                            Log.e(TAG, "3MTH : " + msg.obj);
                            JSONArray arr = new JSONArray(String.valueOf(msg.obj));
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                if (i != 0) {
                                    String key = /*bzTime(obj.getString("timestamp"))*/obj.getString("timestamp").substring(5, 10).replace("-", ".");

                                    double usage = Double.parseDouble(obj.getString("EnergyP")) - Double.parseDouble(arr.getJSONObject(i - 1).getString("EnergyP"));
                                    usage = usage < 0 ? 0 : usage;
                                    Chart_3MTH.get(key).setUsage(usage);
                                    Chart_3MTH.get(key).setCost(usage * deviceInfo.getPrice());
                                }
                            }
                        }
                        for (int i = 0; i < XLabel.length; i++) {
                            xRawDatas.add(XLabel[i]);
                            usageList.add(Chart_3MTH.get(XLabel[i]).getUsage());
                            costList.add(Chart_3MTH.get(XLabel[i]).getUsage());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case REQUEST_6MTH: {
                    try {
                        if (msg.arg2 != 2) {
                            JSONArray arr = new JSONArray(String.valueOf(msg.obj));
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                if (i != 0) {
                                    String key = bzTime(obj.getString("timestamp")).substring(5, 10).replace("-", ".");
                                    double usage = Double.parseDouble(obj.getString("EnergyP")) - Double.parseDouble(arr.getJSONObject(i - 1).getString("EnergyP"));
                                    usage = usage < 0 ? 0 : usage;
                                    Chart_6MTH.get(key).setUsage(usage);
                                    Chart_6MTH.get(key).setCost(usage * deviceInfo.getPrice());
                                }
                            }
                        }
                        for (int i = 0; i < XLabel.length; i++) {
                            xRawDatas.add(XLabel[i]);
                            usageList.add(Chart_6MTH.get(XLabel[i]).getUsage());
                            costList.add(Chart_6MTH.get(XLabel[i]).getUsage());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case REQUEST_1YR: {
                    if (msg.arg2 != 2) {
                        Log.e(TAG, "1YR : " + msg.obj);
                        try {
                            JSONArray arr = new JSONArray(String.valueOf(msg.obj));
                            double LastData = 0;
                            Calendar lastcalendar = null;
                            for (int i = 0; i < arr.length(); i++) {
                                //timestamp
                                JSONObject obj = arr.getJSONObject(i);
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(iso8601_format.parse(obj.getString("timestamp")));
                                calendar.add(Calendar.HOUR_OF_DAY, timeZone());
                                if (lastcalendar != null && calendar.get(Calendar.MONTH) - lastcalendar.get(Calendar.MONTH) == 1) {
                                    double theData = Double.parseDouble(obj.getString("EnergyP")) - LastData;
                                    theData = theData < 0 ? 0 : theData;
                                    Chart_1YR.get(String.valueOf(lastcalendar.get(Calendar.MONTH) + 1)).setUsage(theData);
                                    Chart_1YR.get(String.valueOf(lastcalendar.get(Calendar.MONTH) + 1)).setCost(theData * deviceInfo.getPrice());
                                    lastcalendar = calendar;
                                    LastData = theData;
                                    Log.e(TAG, "   " + theData);
                                } else if (lastcalendar == null) {
                                    LastData = Double.parseDouble(obj.getString("EnergyP"));
                                    lastcalendar = calendar;
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    for (int i = 0; i < XLabel.length; i++) {
                        xRawDatas.add(XLabel[i]);
                        usageList.add(Chart_1YR.get(XLabel[i]).getUsage());
                        costList.add(Chart_1YR.get(XLabel[i]).getUsage());
                    }
                    break;
                }
                case REQUEST_2YR: {
                    Log.e(TAG, "2YR : " + msg.obj);
                    try {
                        JSONArray arr = new JSONArray(String.valueOf(msg.obj));
                        double LastData = 0;
                        Calendar lastcalendar = null;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(iso8601_format.parse(obj.getString("timestamp")));
                            calendar.add(Calendar.HOUR_OF_DAY, timeZone());
                            if (lastcalendar != null && calendar.get(Calendar.MONTH) - lastcalendar.get(Calendar.MONTH) == 1) {
                                double theData = Double.parseDouble(obj.getString("EnergyP")) - LastData;
                                theData = theData < 0 ? 0 : theData;
                                //Log.e(TAG,"  "+String.valueOf(lastcalendar.get(Calendar.YEAR)+"."+(lastcalendar.get(Calendar.MONTH)+1)));
                                Chart_2YR.get(String.valueOf(lastcalendar.get(Calendar.YEAR) + "." + (lastcalendar.get(Calendar.MONTH) + 1))).setUsage(theData);
                                Chart_2YR.get(String.valueOf(lastcalendar.get(Calendar.YEAR) + "." + (lastcalendar.get(Calendar.MONTH) + 1))).setCost(theData * deviceInfo.getPrice());
                                lastcalendar = calendar;
                                LastData = theData;
                                Log.e(TAG, "   " + theData);
                            } else if (lastcalendar == null) {
                                LastData = Double.parseDouble(obj.getString("EnergyP"));
                                lastcalendar = calendar;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < XLabel.length; i++) {
                        xRawDatas.add(XLabel[i]);
                        //Log.e(TAG, "XLabel:   " + XLabel[i]);
                        usageList.add(Chart_2YR.get(XLabel[i]).getUsage());
                        costList.add(Chart_2YR.get(XLabel[i]).getUsage());
                    }
                    break;
                }
            }
            int max, average;
            if (usageList.isEmpty()) {
                max = 1;
                average = 1;
            } else {
                if (view1 != null) {
                    summaryChart1.refreshUI(xRawDatas, usageList, title);
                    view1.repaint();
                }
            }

            if (costList.isEmpty()) {
                max = 1;
                average = 1;
            } else {
                if (view2 != null) {
                    summaryChart2.refreshUI(xRawDatas, costList, title);
                    view2.repaint();
                }
            }

            loadata.setVisibility(View.INVISIBLE);
            summary.setEnabled(true);
        }
    };

    private String title = "";


    public String[] XLabel = null;


    private String t_begin = null, t_end = null;

    Runnable httpRunnable = new Runnable() {
        @Override
        public void run() {
            Message message = handler.obtainMessage();
            try {
                String url = HTTPManagementAPI.WISLINK_URL + "getdata/" + uuid + "?t_begin=" + t_begin + "&t_end=" + t_end + "&sample=" + sample;
                Log.e(TAG, "url:  " + url);
                Map<String, String> map = new HashMap<>();
                map.put("wislink_auth_uuid", wislink_uuid);
                map.put("wislink_auth_token", wislink_token);
                String data = httpAPI.getMethodGET(url, map, true);
                message.obj = data;
                message.what = REQUEST_PARAMETER;
            } catch (Exception e) {
                e.printStackTrace();
                message.what = REQUEST_FAIL;
            }
            handler.sendMessage(message);
        }
    };
    private int sample = 0;

    private String bzTime(String timestamp) {
        try {
            Date date = iso8601_format.parse(timestamp);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.HOUR_OF_DAY, timeZone());
            return iso8601_format.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back) {
            finish();
        } else if (v.getId() == R.id.previous) {
            summaryType = true;
            viewPage(0);
            previous.setBackgroundResource(R.drawable.summary_nor);
            previous.setEnabled(false);
            next.setBackgroundResource(R.drawable.summary_prs);
            next.setEnabled(true);

            history.setText(getString(R.string.usage_history));
            //initSummary();
        } else if (v.getId() == R.id.next) {
            history.setText(getString(R.string.cost_history));

            summaryType = false;
            viewPage(1);
            previous.setBackgroundResource(R.drawable.summary_prs);
            previous.setEnabled(true);
            next.setBackgroundResource(R.drawable.summary_nor);
            next.setEnabled(false);
            //initSummary();
        } else {
            if (uuid == null) {
                Log.e(TAG, "请激活该设备...");
                return;
            }
            usageList.clear();
            xRawDatas.clear();
            summary.setEnabled(false);

            for (TextView tab : tabMenu) {
                tab.setTextColor(getResources().getColor(R.color.white));
            }

            switch (v.getId()) {
                case R.id.hr:
                    hr.setTextColor(getResources().getColor(R.color.green));
                    XLabel = new String[8];
                    calendar.setTime(new Date());//设置时间为当前时间
                    for (int i = 0; i < XLabel.length - 1; i++) {
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);
                        if (i < 6) {
                            XLabel[i] = decimalFormat.format(hour-1) + ":" + i + "0";
                        } else {
                            XLabel[i] = decimalFormat.format(hour) + ":00";
                        }
                        Log.e(TAG, "XLabel[i]  " + XLabel[i]);
                    }
                    calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - timeZone());//标准时间
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 10);
                    calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
                    t_end = iso8601_format.format(calendar.getTime());

                    calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - 20);
                    calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - 1);
                    t_begin = iso8601_format.format(calendar.getTime());

                    REQUEST_PARAMETER = REQUEST_HR;
                    sample = XLabel.length;
                    calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - 20);
                    title = dynamicFormatTime("yyyy-MM-dd HH", calendar);
                    if (Chart_HR.isEmpty()) {
                        for (int i = 0; i < XLabel.length - 1; i++) {
                            ChartBattery battery = new ChartBattery();
                            battery.setKey(XLabel[i]);
                            Chart_HR.put(battery.getKey(), battery);
                        }
                        pool.execute(httpRunnable);
                    } else {
                        Message msg = handler.obtainMessage();
                        msg.what = REQUEST_HR;
                        msg.arg2 = 2;
                        handler.sendMessage(msg);
                    }
                    loadata.setVisibility(View.VISIBLE);
                    break;
                case R.id.day:
                    day.setTextColor(getResources().getColor(R.color.green));
                    XLabel = new String[24];
                    for (int i = 0; i < 24; i++) {
                        XLabel[i] = String.valueOf(i);
                    }
                    calendar.setTime(new Date());//设置时间为当前时间
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    t_begin = iso8601_format.format(calendar.getTime());

                    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - 1);
                    t_end = iso8601_format.format(calendar.getTime());
                    title = dynamicFormatTime("yyyy-MM-dd", calendar);
                    REQUEST_PARAMETER = REQUEST_DAY;
                    sample = Chart_DAY.size();
                    if (Chart_DAY.isEmpty()) {
                        for (int i = 0; i < XLabel.length; i++) {
                            ChartBattery battery = new ChartBattery();
                            battery.setKey(XLabel[i]);
                            Chart_DAY.put(battery.getKey(), battery);
                        }
                        pool.execute(httpRunnable);
                    } else {
                        Message msg = handler.obtainMessage();
                        msg.what = REQUEST_DAY;
                        msg.arg2 = 2;
                        handler.sendMessage(msg);
                    }
                    loadata.setVisibility(View.VISIBLE);
                    break;
                case R.id.wk:
                    wk.setTextColor(getResources().getColor(R.color.green));
                    calendar.setTime(new Date());//设置时间为当前时间
                    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - 1);
                    t_end = iso8601_format.format(calendar.getTime());
                    String start = dynamicFormatTime("yyyy-MM-dd", calendar);
                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 1);
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) - 7);
                    title = dynamicFormatTime("yyyy-MM-dd", calendar) + " ~ " + start;
                    t_begin = iso8601_format.format(calendar.getTime());
                    REQUEST_PARAMETER = REQUEST_WK;

                    XLabel = new String[7];
                    for (int i = 0; i < 7; i++) {
                        XLabel[i] = String.valueOf(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) + 1);
                    }
                    sample = XLabel.length;

                    if (Chart_WK.isEmpty()) {
                        for (int i = 0; i < XLabel.length; i++) {
                            ChartBattery battery = new ChartBattery();
                            battery.setKey(XLabel[i]);
                            Chart_WK.put(battery.getKey(), battery);
                        }
                        pool.execute(httpRunnable);
                    } else {
                        Message msg = handler.obtainMessage();
                        msg.what = REQUEST_WK;
                        msg.arg2 = 2;
                        handler.sendMessage(msg);
                    }
                    loadata.setVisibility(View.VISIBLE);
                    break;
                case R.id.mth:
                    mth.setTextColor(getResources().getColor(R.color.green));
                    XLabel = new String[getMonthDay(1)];
                    for (int i = 0; i < XLabel.length; i++) {
                        XLabel[i] = String.valueOf(decimalFormat.format(i + 1));
                    }
                    calendar.setTime(new Date());//设置时间为当前时间
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - 1);
                    t_end = iso8601_format.format(calendar.getTime());
                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 1);
                    calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
                    t_begin = iso8601_format.format(calendar.getTime());
                    REQUEST_PARAMETER = REQUEST_MTH;

                    sample = XLabel.length;

                    if (Chart_MTH.isEmpty()) {
                        for (int i = 0; i < XLabel.length; i++) {
                            ChartBattery battery = new ChartBattery();
                            battery.setKey(XLabel[i]);
                            Chart_MTH.put(battery.getKey(), battery);
                        }
                        pool.execute(httpRunnable);
                    } else {
                        Message msg = handler.obtainMessage();
                        msg.what = REQUEST_MTH;
                        msg.arg2 = 2;
                        handler.sendMessage(msg);
                    }
                    title = dynamicFormatTime("yyyy-MM", calendar);
                    loadata.setVisibility(View.VISIBLE);
                    break;
                case R.id.mth3:
                    mth3.setTextColor(getResources().getColor(R.color.green));
                    calendar.setTime(new Date());//设置时间为当前时间
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - 1);
                    t_end = iso8601_format.format(calendar.getTime());

                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 1);
                    calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 3);
                    t_begin = iso8601_format.format(calendar.getTime());
                    REQUEST_PARAMETER = REQUEST_3MTH;
                    int L3_1 = getMonthDay(1);
                    int L3_2 = getMonthDay(2);
                    int L3_3 = getMonthDay(3);
                    int M3_1 = calendar.get(Calendar.MONTH) + 1;
                    XLabel = new String[L3_1 + L3_2 + L3_3];
                    for (int i = 0; i < XLabel.length; i++) {
                        if (i < L3_3) {
                            XLabel[i] = decimalFormat.format(M3_1) + "." + decimalFormat.format(i + 1);
                        } else if (i < L3_3 + L3_2) {
                            XLabel[i] = decimalFormat.format(M3_1 + 1 > 12 ? (M3_1 + 1 - 12) : (M3_1 + 1)) + "." + decimalFormat.format(i - L3_3 + 1);
                        } else if (i < L3_3 + L3_2 + L3_1) {
                            XLabel[i] = decimalFormat.format(M3_1 + 2 > 12 ? (M3_1 + 2 - 12) : (M3_1 + 2)) + "." + decimalFormat.format(i - L3_3 - L3_2 + 1);
                        }
                    }
                    sample = XLabel.length;
                    String start_m = dynamicFormatTime("yyyy-MM", calendar);
                    calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 2);
                    String end_m = dynamicFormatTime("yyyy-MM", calendar);
                    title = start_m + " ~ " + end_m;

                    if (Chart_3MTH.isEmpty()) {
                        for (int i = 0; i < XLabel.length; i++) {
                            ChartBattery battery = new ChartBattery();
                            battery.setKey(String.valueOf(XLabel[i]));
                            Chart_3MTH.put(battery.getKey(), battery);
                        }
                        pool.execute(httpRunnable);
                    } else {
                        Message msg = handler.obtainMessage();
                        msg.what = REQUEST_3MTH;
                        msg.arg2 = 2;
                        handler.sendMessage(msg);
                    }
                    loadata.setVisibility(View.VISIBLE);
                    break;
                case R.id.mth6:
                    mth6.setTextColor(getResources().getColor(R.color.green));
                    calendar.setTime(new Date());//设置时间为当前时间
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - 1);
                    t_end = iso8601_format.format(calendar.getTime());
                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 1);
                    calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
                    t_begin = iso8601_format.format(calendar.getTime());
                    REQUEST_PARAMETER = REQUEST_6MTH;
                    int L6_1 = getMonthDay(1);
                    int L6_2 = getMonthDay(2);
                    int L6_3 = getMonthDay(3);
                    int L6_4 = getMonthDay(4);
                    int L6_5 = getMonthDay(5);
                    int L6_6 = getMonthDay(6);
                    int M6_1 = calendar.get(Calendar.MONTH) + 1;
                    XLabel = new String[L6_1 + L6_2 + L6_3 + L6_4 + L6_5 + L6_6];
                    for (int i = 0; i < XLabel.length; i++) {
                        if (i < L6_6) {
                            XLabel[i] = decimalFormat.format(M6_1) + "." + decimalFormat.format(i + 1);
                        } else if (i < L6_6 + L6_5) {
                            XLabel[i] = decimalFormat.format(M6_1 + 1 > 12 ? (M6_1 + 1 - 12) : (M6_1 + 1)) + "." + decimalFormat.format(i - L6_6 + 1);
                        } else if (i < L6_6 + L6_5 + L6_4) {
                            XLabel[i] = decimalFormat.format(M6_1 + 2 > 12 ? (M6_1 + 2 - 12) : (M6_1 + 2)) + "." + decimalFormat.format(i - L6_6 - L6_5 + 1);
                        } else if (i < L6_6 + L6_5 + L6_4 + L6_3) {
                            XLabel[i] = decimalFormat.format(M6_1 + 3 > 12 ? (M6_1 + 3 - 12) : (M6_1 + 3)) + "." + decimalFormat.format(i - L6_6 - L6_5 - L6_4 + 1);
                        } else if (i < L6_6 + L6_5 + L6_4 + L6_3 + L6_2) {
                            XLabel[i] = decimalFormat.format(M6_1 + 4 > 12 ? (M6_1 + 4 - 12) : (M6_1 + 4)) + "." + decimalFormat.format(i - L6_6 - L6_5 - L6_4 - L6_3 + 1);
                        } else if (i < L6_6 + L6_5 + L6_4 + L6_3 + L6_2 + L6_1) {
                            XLabel[i] = decimalFormat.format(M6_1 + 5 > 12 ? (M6_1 + 5 - 12) : (M6_1 + 5)) + "." + decimalFormat.format(i - L6_6 - L6_5 - L6_4 - L6_3 - L6_2 + 1);
                        }
                    }
                    sample = XLabel.length;
                    String start_6m = dynamicFormatTime("yyyy-MM", calendar);
                    calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 5);
                    String end_6m = dynamicFormatTime("yyyy-MM", calendar);
                    title = start_6m + " ~ " + end_6m;
                    if (Chart_6MTH.isEmpty()) {
                        for (int i = 0; i < XLabel.length; i++) {
                            ChartBattery battery = new ChartBattery();
                            battery.setKey(String.valueOf(XLabel[i]));
                            Chart_6MTH.put(battery.getKey(), battery);
                        }
                        pool.execute(httpRunnable);
                    } else {
                        Message msg = handler.obtainMessage();
                        msg.what = REQUEST_6MTH;
                        msg.arg2 = 2;
                        handler.sendMessage(msg);
                    }
                    loadata.setVisibility(View.VISIBLE);
                    break;
                case R.id.yr1:
                    yr1.setTextColor(getResources().getColor(R.color.green));
                    XLabel = new String[12];
                    for (int i = 0; i < 12; i++) {
                        XLabel[i] = String.valueOf(i + 1);
                    }
                    calendar.setTime(new Date());//设置时间为当前时间
                    calendar.set(Calendar.MONTH, Calendar.JANUARY);
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - 1);
                    t_end = iso8601_format.format(calendar.getTime());

                    calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 1);
                    calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
                    t_begin = iso8601_format.format(calendar.getTime());
                    REQUEST_PARAMETER = REQUEST_1YR;
                    sample = Chart_1YR.size();
                    title = dynamicFormatTime("yyyy", calendar);

                    if (Chart_1YR.isEmpty()) {
                        for (int i = 0; i < XLabel.length; i++) {
                            ChartBattery battery = new ChartBattery();
                            battery.setKey(XLabel[i]);
                            Chart_1YR.put(battery.getKey(), battery);
                        }
                        pool.execute(httpRunnable);
                    } else {
                        Message msg = handler.obtainMessage();
                        msg.what = REQUEST_1YR;
                        msg.arg2 = 2;
                        handler.sendMessage(msg);
                    }
                    loadata.setVisibility(View.VISIBLE);
                    break;
                case R.id.yr2:
                    yr2.setTextColor(getResources().getColor(R.color.green));
                    sample = 24;
                    calendar.setTime(new Date());//设置时间为当前时间
                    calendar.set(Calendar.MONTH, Calendar.JANUARY);
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    t_end = iso8601_format.format(calendar.getTime());
                    REQUEST_PARAMETER = REQUEST_2YR;
                    calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 2);
                    t_begin = iso8601_format.format(calendar.getTime());
                    Log.e(TAG, "year:  " + calendar.get(Calendar.YEAR));
                    XLabel = new String[sample];
                    for (int i = 0; i < XLabel.length; i++) {
                        XLabel[i] = (i > 11 ? (calendar.get(Calendar.YEAR) + 1) : calendar.get(Calendar.YEAR)) + "." + (i > 11 ? (i - 11) : (i + 1));
                    }
                    if (Chart_2YR.isEmpty()) {
                        for (int i = 0; i < XLabel.length; i++) {
                            ChartBattery battery = new ChartBattery();
                            battery.key = XLabel[i];
                            Chart_2YR.put(battery.getKey(), battery);
                        }
                        pool.execute(httpRunnable);
                    } else {
                        Message msg = handler.obtainMessage();
                        msg.what = REQUEST_2YR;
                        msg.arg2 = 2;
                        handler.sendMessage(msg);
                    }
                    pool.execute(httpRunnable);
                    break;
            }
        }
    }


    /**
     * 获取上 i 个月的天数
     *
     * @param i
     * @return
     */
    public static int getMonthDay(int i) {
        Calendar a = Calendar.getInstance();
        a.set(Calendar.MONTH, a.get(Calendar.MONTH) - i);
        a.set(Calendar.DATE, 1);
        a.roll(Calendar.DATE, -1);
        return a.get(Calendar.DATE);
    }

    private void viewPage(int index) {
        ViewPagerScroller scroller = new ViewPagerScroller(this);
        scroller.initViewPagerScroll(viewpage);
        viewpage.setCurrentItem(index, true);
    }

    class ChartBattery {
        private String key;
        private double usage;
        private double cost;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public double getUsage() {
            return usage;
        }

        public void setUsage(double usage) {
            this.usage = usage;
        }

        public double getCost() {
            return cost;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }
    }
}
