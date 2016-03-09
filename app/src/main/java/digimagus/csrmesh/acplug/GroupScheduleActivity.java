package digimagus.csrmesh.acplug;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import digimagus.csrmesh.adapter.GroupScheduleAdapter;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.GroupDevice;
import digimagus.csrmesh.entities.ScheduleInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;
import digimagus.csrmesh.view.SSwipeRefreshLayout;

/**
 * group schedule
 */
public class GroupScheduleActivity extends BaseActivity implements View.OnClickListener, GroupScheduleAdapter.ControlScheduleListener {
    private final static String TAG = "GroupScheduleActivity";

    private ImageView back;
    private SSwipeRefreshLayout refreshLayout;
    private ListView scheduleList;
    private TextView add,name_title;
    private GroupScheduleAdapter groupScheduleAapter;
    private Map<String, Map<Integer, ScheduleInfo>> schedules = new ConcurrentHashMap<>();
    private int groupId;
    private List<ScheduleInfo> groupSchedules = new ArrayList<>();
    private DisplayMetrics dm = new DisplayMetrics();

    Map<String, DeviceInfo> deviceInfoMap = new ConcurrentHashMap<>();

    @Override
    protected void handler(Message msg) {
        try {
            switch (msg.what) {
                case DEVICE_BACK_SCHEDULE:
                    JSONObject obj = new JSONObject(String.valueOf(msg.obj));
                    String serial;
                    if (obj.has("topic")) {
                        obj = obj.getJSONObject("data");
                        serial = mUUIDSERIAL.get(obj.getString("fromUuid"));
                    } else {
                        serial = obj.getString("devices");
                    }
                    schedule(serial, obj.getJSONObject("payload"));
                    Log.e(TAG, "udp: " + msg.obj);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_groupschedule);
        groupId = getIntent().getIntExtra("groupId", 0);
        progressDialog = new ProgressBarDialog(this).createDialog(this);
        progressDialog.setMessage(getString(R.string.query_schedule));
        initFindViewById();
    }

    private void initFindViewById() {
        back = (ImageView) findViewById(R.id.back);
        add = (TextView) findViewById(R.id.add);
        name_title= (TextView) findViewById(R.id.name_title);
        refreshLayout = (SSwipeRefreshLayout) findViewById(R.id.refreshLayout);
        scheduleList = (ListView) findViewById(R.id.scheduleList);
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        groupScheduleAapter = new GroupScheduleAdapter(this, groupSchedules, dm.widthPixels);
        scheduleList.setAdapter(groupScheduleAapter);
        back.setOnClickListener(this);
        add.setOnClickListener(this);
        refreshLayout.setOnRefreshListener(refreshListener);
    }

    SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshLayout.setRefreshing(false);
        }
    };
    private boolean addschedule = false;
    private boolean deleteschedule = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.add:
                //请求Schedule
                /**
                 * 1、得到组中的设备
                 * 2、设置为增加schedule
                 * 3、请求组中每个设备的schedule
                 * 4、做个延迟，8s钟若没有请求到数据
                 */
                progressDialog.show();
                Map<String, DeviceInfo> devices = mGroupDevices.get(groupId).getDevices();
                for (DeviceInfo d : devices.values()) {
                    deviceInfoMap.put(d.getSerial(), d);
                }
                addschedule = true;

                getGDeviceSchedule(groupId);
                if (devices.size() > 0) {
                    handler.postDelayed(delayRunnable, 8 * 1000);
                }
                Log.e(TAG, "添加Schedule size:" + deviceInfoMap.size() + "   groupid:" + groupId);
                break;
        }
    }

    private ScheduleInfo deleteSchedule;

    @Override
    public void controlSchedule(int position, View v) {
        switch (v.getId()) {
            case R.id.delete:
                progressDialog.show();
                Map<String, DeviceInfo> devices = mGroupDevices.get(groupId).getDevices();
                for (DeviceInfo d : devices.values()) {
                    deviceInfoMap.put(d.getSerial(), d);
                }
                deleteschedule = true;

                getGDeviceSchedule(groupId);
                if (devices.size() > 0) {
                    handler.postDelayed(delayRunnable, 8 * 1000);
                }
                deleteSchedule = groupSchedules.get(position);
                break;
        }
    }

    Handler msgHandler = new Handler();
    Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            progressDialog.dismiss();
        }
    };

    Runnable delayRunnable = new Runnable() {
        @Override
        public void run() {
            progressDialog.dismiss();
            handler.removeCallbacks(delayRunnable);
            if (addschedule) {
                Tips(getString(R.string.group_offonline)).setPositiveButton(R.string.yes, null).create().show();
            } else if (deleteschedule) {
                Tips(getString(R.string.group_offonline)).setPositiveButton(R.string.yes, null).create().show();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "请求组中每个Device Schedule的定时计划");
        //请求组的Schedule  请求每个设备的Schedule

        GroupDevice groupDevice=mGroupDevices.get(groupId);

        name_title.setText(groupDevice.getName());

        groupScheduleAapter.setControlScheduleListener(this);
        getGDeviceSchedule(groupId);
        Log.e(TAG, "groupId： " + groupId);
        groupSchedules = mDeviceStore.getGroupSchedulesById(groupId);
        if (groupSchedules != null) {
            groupScheduleAapter.notifyDataSetChanged(groupSchedules);
        }
        progressDialog.show();
        msgHandler.postDelayed(timeRunnable, 8 * 1000);
    }

    private ProgressBarDialog progressDialog;

    @Override
    protected void onPause() {
        super.onPause();
        msgHandler.removeCallbacks(timeRunnable);
    }

    public void schedule(String serial, JSONObject obj) {
        saveSchedule(serial, obj);
        deviceInfoMap.remove(serial);
        if (deviceInfoMap.isEmpty()) {
            if (addschedule) {//添加schedule
                handler.removeCallbacks(delayRunnable);
                progressDialog.dismiss();
                addschedule = false;
                /**
                 * 1、判断查询的schedule的个数是否大于10 大于10 提示客户大于10 否则跳转至新增GroupSchedule界面
                 */
                int size = 0;
                String deviceserial = null;
                for (Map.Entry<String, Map<Integer, ScheduleInfo>> entry : schedules.entrySet()) {
                    if (entry.getValue().size() >= 10) {
                        size = entry.getValue().size();
                        if (deviceserial == null) {
                            deviceserial = entry.getKey();
                        } else {
                            deviceserial += "," + entry.getKey();
                        }
                    }
                }
                if (size >= 10) {
                    //提示用户的schedule的个数大于10
                    Tips(getString(R.string.save_schedule_10) + "(" + devices.get(deviceserial).getName() + ")").setPositiveButton(R.string.yes, null).create().show();
                } else {
                    Intent intent = new Intent(GroupScheduleActivity.this, AddGroupScheduleActivity.class);
                    intent.putExtra("groupId", groupId);
                    startActivity(intent);
                }
            } else if (deleteschedule) {
                handler.removeCallbacks(delayRunnable);
                progressDialog.dismiss();
                deleteschedule = false;

                for (Map.Entry<String, Map<Integer, ScheduleInfo>> entry : schedules.entrySet()) {
                    for (Map.Entry<Integer, ScheduleInfo> ent : entry.getValue().entrySet()) {
                        ScheduleInfo schedule = ent.getValue();
                        if (deleteSchedule.start_h == schedule.start_h &&
                                deleteSchedule.start_m == schedule.start_m &&
                                deleteSchedule.start_w == schedule.start_w &&
                                deleteSchedule.end_h == schedule.end_h &&
                                deleteSchedule.end_m == schedule.end_m &&
                                deleteSchedule.end_w == schedule.end_w &&
                                deleteSchedule.repeat == schedule.repeat &&
                                deleteSchedule.start_s == schedule.start_s &&
                                deleteSchedule.end_s == schedule.end_s) {

                            Log.e(TAG, "serail: " + entry.getKey());

                            StringBuffer sb = new StringBuffer();
                            sb.append("{\"schedule\":{\"index\":");
                            sb.append(schedule.index);
                            sb.append(",\"start\":[0,0,0,0]");
                            sb.append(",\"end\":[0,0,0,0]");
                            sb.append(",\"duration\":0");
                            sb.append(",\"repeat\":0");
                            sb.append(",\"enable\":0");
                            sb.append("}}");

                            setSchedule(entry.getKey(), sb.toString());
                        }
                    }
                }
                Log.e(TAG, "scheduleInfo:" + deleteSchedule.toString());

                mDeviceStore.removeGroupSchedule(deleteSchedule.id);
                groupSchedules.remove(deleteSchedule);
                groupScheduleAapter.notifyDataSetChanged(groupSchedules);
            }
        }
        msgHandler.removeCallbacks(timeRunnable);
        progressDialog.dismiss();
    }

    private void saveSchedule(String serial, JSONObject jobj) {
        try {
            JSONArray arr = jobj.getJSONArray("schedule");
            Map<Integer, ScheduleInfo> schedule = new HashMap<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ScheduleInfo info = new ScheduleInfo();
                info.index = (obj.getInt("index"));
                info.enable = (obj.getInt("enable"));
                info.repeat = (obj.getInt("repeat"));
                info.running = (obj.getInt("running"));
                JSONArray start = obj.getJSONArray("start");
                info.start_h = start.getInt(0);
                info.start_m = start.getInt(1);
                info.start_w = start.getInt(2);
                info.start_s = start.getInt(3);
                JSONArray end = obj.getJSONArray("end");
                info.end_h = end.getInt(0);
                info.end_m = end.getInt(1);
                info.end_w = end.getInt(2);
                info.end_s = start.getInt(3);
                schedule.put(Integer.valueOf(info.index), info);
            }
            schedules.put(serial, schedule);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private CustomDialog.Builder ibuilder;

    private CustomDialog.Builder Tips(String msg) {
        ibuilder = new CustomDialog.Builder(this);
        ibuilder.setTitle(R.string.tips);
        ibuilder.setMessage(msg);
        return ibuilder;
    }


    @Override
    public void setScheduleEnable(String schedule) {

    }
}
