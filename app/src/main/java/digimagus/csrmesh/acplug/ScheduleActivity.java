package digimagus.csrmesh.acplug;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import digimagus.csrmesh.adapter.SlipSMenuAdapter;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.ScheduleInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;
import digimagus.csrmesh.view.SSwipeRefreshLayout;

/**
 * 定时计划
 */
public class ScheduleActivity extends BaseActivity implements View.OnClickListener, SlipSMenuAdapter.ControlScheduleListener {
    private final static String TAG = "ScheduleActivity";
    private ImageView back;

    private TextView add;
    private SlipSMenuAdapter scheduleAdapter = null;
    private SSwipeRefreshLayout refreshLayout;
    private ListView scheduleList;

    private List<ScheduleInfo> scheduleInfos = new ArrayList<>();
    private Map<Integer, ScheduleInfo> schedules = new ConcurrentHashMap<>();

    private DisplayMetrics dm = new DisplayMetrics();
    private DeviceInfo deviceInfo;
    private String serial;
    private ProgressBarDialog progressDialog;


    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_schedule);
        serial = getIntent().getBundleExtra("bundle").getString("serial");
        initFindViewById();
    }

    private void initFindViewById() {
        back = (ImageView) findViewById(R.id.back);
        add = (TextView) findViewById(R.id.add);
        scheduleList = (ListView) findViewById(R.id.scheduleList);
        back.setOnClickListener(this);
        add.setOnClickListener(this);
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        scheduleAdapter = new SlipSMenuAdapter(this, scheduleInfos, dm.widthPixels);
        scheduleList.setAdapter(scheduleAdapter);
        scheduleAdapter.setControlScheduleListener(this);
        refreshLayout = (SSwipeRefreshLayout) findViewById(R.id.refreshLayout);
        refreshLayout.setViewGroup(scheduleList);
        refreshLayout.setOnRefreshListener(refreshListener);
    }

    SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            data = false;
            scheduleInfos.clear();
            schedules.clear();
            scheduleAdapter.notifyDataSetChanged();

            deviceInfo = devices.get(serial);
            if (deviceInfo != null && deviceInfo.online) {
                queryDeviceState(deviceInfo, 4);
                msgHandler.postDelayed(timeRunnable, 8 * 1000);
            } else {
                Toast.makeText(ScheduleActivity.this, getString(R.string.device_not_online), Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        showProgressDialog(getString(R.string.query_schedule)).show();
        data = false;
        scheduleInfos.clear();
        schedules.clear();
        scheduleAdapter.notifyDataSetChanged();
        deviceInfo = devices.get(serial);
        if (deviceInfo != null && deviceInfo.online) {
            queryDeviceState(deviceInfo, 4);
            msgHandler.postDelayed(timeRunnable, 8 * 1000);
        } else {
            progressDialog.dismiss();
            Toast.makeText(ScheduleActivity.this, getString(R.string.device_not_online), Toast.LENGTH_SHORT).show();
        }
    }

    Handler msgHandler = new Handler();
    Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            refreshLayout.setRefreshing(false);
            progressDialog.dismiss();
            tips(getString(R.string.timeout)).setPositiveButton(R.string.yes, null).create().show();
        }
    };

    private boolean data = false;

    @Override
    protected void onPause() {
        super.onPause();
        progressDialog.dismiss();
        msgHandler.removeCallbacks(timeRunnable);
        scheduleInfos.clear();
        schedules.clear();
    }

    @Override
    public void controlSchedule(int position, View v) {
        switch (v.getId()) {
            case R.id.delete:
                try {
                    if(schedules.isEmpty()){
                        queryDeviceState(deviceInfo, 4);
                    }else{
                        ScheduleInfo info = scheduleInfos.get(position);
                        JSONObject obj = new JSONObject(info.json);
                        StringBuffer sb = new StringBuffer();
                        sb.append("{\"schedule\":{\"index\":");
                        sb.append(info.index);
                        sb.append(",\"start\":");
                        sb.append(obj.getString("start"));
                        sb.append(",\"end\":");
                        sb.append(obj.getString("end"));
                        sb.append(",\"duration\":0");
                        sb.append(",\"repeat\":1,\"enable\":0");
                        sb.append("}");
                        sb.append("}");
                        final StringBuffer json = sb;
                        Log.e(TAG, "DELETE JSON " + json.toString());
                        tips(getString(R.string.remove_schedule)).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                showProgressDialog(getString(R.string.delete_schedule)).show();
                                setSchedule(serial, json.toString());
                            }
                        }).setNegativeButton(R.string.no, null).create().show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    progressDialog.dismiss();
                }
                break;
            case R.id.content: {
                if (scheduleInfos.isEmpty()) {
                    return;
                }
                ScheduleInfo info = scheduleInfos.get(position);
                if (info != null) {
                    Intent intent = new Intent(ScheduleActivity.this, DeviceScheduleActivity.class);
                    intent.putExtra("type", "update");
                    intent.putExtra("serial", serial);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("info", info);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
                break;
            }
        }
    }

    @Override
    public void setScheduleEnable(String schedule) {
        Log.e(TAG, "setScheduleEnable   " + schedule);
        setSchedule(serial, zh(schedule));
        showProgressDialog(getString(R.string.update_schedule)).show();
        msgHandler.postDelayed(timeRunnable, 8 * 1000);
    }

    private String zh(String zh) {
        StringBuffer sb = new StringBuffer();
        sb.append("{\"schedule\":");
        sb.append(zh);
        sb.append("}");
        return sb.toString();
    }

    private CustomDialog.Builder tips(String msg) {
        ibuilder = new CustomDialog.Builder(this);
        ibuilder.setTitle(R.string.tips);
        ibuilder.setMessage(msg);
        return ibuilder;
    }

    private CustomDialog.Builder ibuilder;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.add:
                deviceInfo = devices.get(serial);
                if (!deviceInfo.online) {
                    Toast.makeText(ScheduleActivity.this, getString(R.string.device_not_online), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!data) {
                    return;
                }
                if (schedules.size() < 10) {
                    Intent intent = new Intent(this, DeviceScheduleActivity.class);
                    intent.putExtra("type", "add");
                    intent.putExtra("serial", serial);
                    startActivity(intent);
                } else {
                    tips(getString(R.string.save_schedule_10)).setPositiveButton(R.string.yes, null).create().show();
                }
                break;
        }
    }

    public enum JSON_TYPE {
        /**
         * JSONObject
         */
        JSON_TYPE_OBJECT,
        /**
         * JSONArray
         */
        JSON_TYPE_ARRAY,
        /**
         * 不是JSON格式的字符串
         */
        JSON_TYPE_ERROR
    }


    private JSON_TYPE getJSONType(String str) {
        if (TextUtils.isEmpty(str)) {
            return JSON_TYPE.JSON_TYPE_ERROR;
        }
        final char[] strChar = str.substring(0, 1).toCharArray();
        final char firstChar = strChar[0];
        if (firstChar == '{') {
            return JSON_TYPE.JSON_TYPE_OBJECT;
        } else if (firstChar == '[') {
            return JSON_TYPE.JSON_TYPE_ARRAY;
        } else {
            return JSON_TYPE.JSON_TYPE_ERROR;
        }
    }

    public void setSuccess(String serial, boolean status) {
        if (!serial.equals(serial)) {
            return;
        }
        if (status) {//设置成功查询设备状态
            showProgressDialog(getString(R.string.query_schedule));
            Log.e(TAG, "QUERY device :" + serial);
            queryDeviceState(deviceInfo, 4);

            msgHandler.postDelayed(timeRunnable, 8 * 1000);
        } else {
            progressDialog.dismiss();
            refreshLayout.setRefreshing(false);
        }
    }

    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void handler(Message msg) {
        Log.e(TAG, msg.what + "  udp:  " + msg.obj);
        try {
            progressDialog.dismiss();
            refreshLayout.setRefreshing(false);
            msgHandler.removeCallbacks(timeRunnable);
            JSONObject obj = new JSONObject(String.valueOf(msg.obj));
            String serial;
            switch (msg.what) {
                case DEVICE_BACK_SCHEDULE://schedule 查询成功
                    if (obj.has("topic")) {
                        obj = obj.getJSONObject("data");
                        serial = mUUIDSERIAL.get(obj.getString("fromUuid"));
                    } else {
                        serial = obj.getString("devices");
                    }
                    obj = obj.getJSONObject("payload");
                    if (obj.has("schedule")) {
                        schedule(serial, obj);
                    }
                    break;
                case PHONE_SET_PARAMEETER_RESULT: {
                    serial = obj.getString("serial");
                    if (serial.equals(deviceInfo.getSerial())) {
                        setSuccess(serial, obj.getInt("result") == 0 ? true : false);
                    }
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void schedule(String serial, JSONObject schedule) {
        msgHandler.removeCallbacks(timeRunnable);
        scheduleInfos.clear();
        schedules.clear();
        refreshLayout.setRefreshing(false);
        if (serial.equals(serial) && schedule != null) {
            try {
                if (JSON_TYPE.JSON_TYPE_ARRAY == getJSONType(schedule.getString("schedule"))) {
                    progressDialog.dismiss();
                    JSONArray arr = schedule.getJSONArray("schedule");
                    calendar.setTimeInMillis(schedule.getLong("timestamp") * 1000);
                    //calendar.setTime(new Date(schedule.getLong("timestamp")*1000));
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        ScheduleInfo info = new ScheduleInfo();
                        info.json = obj.toString();
                        info.index = (obj.getInt("index"));
                        info.enable = (obj.getInt("enable"));
                        info.repeat = (obj.getInt("repeat"));
                        info.running = (obj.getInt("running"));
                        info.settime = obj.getLong("settime");
                        JSONArray start = obj.getJSONArray("start");
                        info.start_h = start.getInt(0);
                        info.start_m = start.getInt(1);
                        info.start_w = start.getInt(2);
                        info.start_s = start.getInt(3);
                        JSONArray end = obj.getJSONArray("end");
                        info.end_h = end.getInt(0);
                        info.end_m = end.getInt(1);
                        info.end_w = end.getInt(2);
                        info.end_s = end.getInt(3);
                        scheduleInfos.add(info);
                        schedules.put(info.index, info);
                    }
                    scheduleAdapter.setTimeCalendar(calendar);
                    scheduleAdapter.notifyDataSetChanged();
                    data = true;
                } else if (JSON_TYPE.JSON_TYPE_OBJECT == getJSONType(schedule.getString("schedule"))) {
                    showProgressDialog(getString(R.string.query_timing_plan));
                    queryDeviceState(deviceInfo, 4);
                    msgHandler.postDelayed(timeRunnable, 8 * 1000);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }


    public ProgressBarDialog showProgressDialog(String msg){
        if(progressDialog!=null&&progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        progressDialog = new ProgressBarDialog(this).createDialog(this).setMessage(msg);
        return progressDialog;
    }
}
