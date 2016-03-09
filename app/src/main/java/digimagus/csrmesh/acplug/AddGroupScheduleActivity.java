package digimagus.csrmesh.acplug;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import digimagus.csrmesh.acplug.util.VerificationSchedule;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.DialogModeInfo;
import digimagus.csrmesh.entities.ScheduleInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.DialogMode;
import digimagus.csrmesh.view.ProgressBarDialog;
import digimagus.csrmesh.view.SwitchView;
import digimagus.csrmesh.view.TimeSelectPopupWindow;

/**
 * 增加 Group Schedule
 */
public class AddGroupScheduleActivity extends BaseActivity implements View.OnClickListener, TimeSelectPopupWindow.ChooseTimeListener {
    private final static String TAG = "AddGroupScheduleActivity";
    private DecimalFormat df = new DecimalFormat("00");
    private TextView on, off, save, weekday, weeks;
    private SwitchView off_switch, on_switch, repeat;
    private ImageView back;
    private TimeSelectPopupWindow timePopupWindow;

    private Calendar calendar = Calendar.getInstance();
    private int start_hour, start_minute, end_hour, end_minute;

    private ScheduleInfo scheduleInfo;

    private int currweek = 0;
    private int groupId;
    private List<DialogModeInfo> modeInfos = new ArrayList<>();
    private DialogMode dialogMode;

    private String[] weeksFullName = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    Map<String, Map<Integer, ScheduleInfo>> schedules = new HashMap<>();

    Map<String, Integer> sendScheduleIndex = new HashMap<>();
    Map<String, StringBuffer> sendSchedule = new HashMap<>();
    private ProgressBarDialog progressDialog;

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_addgroupschedule);
        progressDialog = new ProgressBarDialog(this).createDialog(this);
        progressDialog.setMessage(getString(R.string.setings));
        timePopupWindow = new TimeSelectPopupWindow(AddGroupScheduleActivity.this);
        groupId = getIntent().getIntExtra("groupId", 0);
        initFindViewById();
    }

    @Override
    protected void handler(Message msg) {
        try {
            JSONObject obj = new JSONObject(String.valueOf(msg.obj));
            switch (msg.what) {
                case DEVICE_BACK_SCHEDULE:
                    Log.e(TAG, String.valueOf(msg.obj));
                    String serial;
                    if (obj.has("topic")) {
                        obj = obj.getJSONObject("data");
                        serial = mUUIDSERIAL.get(obj.getString("fromUuid"));
                    } else {
                        serial = obj.getString("devices");
                    }
                    schedule(serial, obj.getJSONObject("payload"));
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void schedule(String serial, JSONObject obj) {
        saveSchedule(serial, obj);
        deviceInfoMap.remove(serial);
        if (deviceInfoMap.isEmpty()) {
            if (addschedule) {//添加schedule
                addschedule = false;
                handler.removeCallbacks(delayRunnable);

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
                    progressDialog.dismiss();

                    //提示用户的schedule的个数大于10
                    Tips(getString(R.string.tips), getString(R.string.save_schedule_10) + "(" + devices.get(deviceserial).getName() + ")").setPositiveButton(R.string.yes, null).create().show();
                } else {
                    //开始验证Schedule 是否有重复
                    Map<String, String> repeatSc = new HashMap<>();
                    for (Map.Entry<String, Map<Integer, ScheduleInfo>> schedul : schedules.entrySet()) {
                        Log.e(TAG, "schedul      :  " + schedul.getValue());
                        Log.e(TAG, "scheduleInfo :  " + scheduleInfo);
                        List<String> repeatSchedule = VerificationSchedule.getInstance().verification(this, "add", scheduleInfo, schedul.getValue());
                        Log.e(TAG, "repeatSchedule      :  " + repeatSchedule);
                        if (!repeatSchedule.isEmpty()) {
                            repeatSc.put(schedul.getKey(), devices.get(schedul.getKey()).getName() + ":" + repeatSchedule);
                        }
                    }

                    Log.e(TAG, "repeatS      :  " + repeatSc);

                    for (Map.Entry<String, Map<Integer, ScheduleInfo>> entry : schedules.entrySet()) {//每个设备添加的索引
                        for (int i = 0; i < 10; i++) {
                            if (entry.getValue().get(Integer.valueOf(i)) == null) {
                                sendScheduleIndex.put(entry.getKey(), i);
                                break;
                            }
                        }
                    }

                    //根据不同的设备封装schedule
                    for (Map.Entry<String, Integer> entry : sendScheduleIndex.entrySet()) {
                        StringBuffer sb = new StringBuffer();
                        sb.append("{\"schedule\":{\"index\":");
                        sb.append(entry.getValue());
                        sb.append(",\"start\":[");
                        sb.append(scheduleInfo.start_h);
                        sb.append(",");
                        sb.append(scheduleInfo.start_m);
                        sb.append(",");
                        sb.append(scheduleInfo.start_w);
                        sb.append(",");
                        sb.append(scheduleInfo.start_s);
                        sb.append("],\"end\":[");
                        sb.append(scheduleInfo.end_h);
                        sb.append(",");
                        sb.append(scheduleInfo.end_m);
                        sb.append(",");
                        sb.append(scheduleInfo.end_w);
                        sb.append(",");
                        sb.append(scheduleInfo.end_s);
                        sb.append("],\"repeat\":");
                        sb.append(scheduleInfo.repeat);
                        sb.append(",\"duration\":");
                        sb.append(scheduleInfo.duration);
                        sb.append(",\"enable\":1");
                        sb.append("}}");
                        sendSchedule.put(entry.getKey(), sb);
                    }

                    if (repeatSc.isEmpty()) {
                        for (Map.Entry<String, StringBuffer> entry : sendSchedule.entrySet()) {
                            Log.e(TAG, "Key : " + entry.getKey() + "    Value: " + entry.getValue().toString());
                            setSchedule(entry.getKey(), entry.getValue().toString());
                        }
                        handler.postDelayed(delayRunnable, 2000);
                    } else {
                        progressDialog.dismiss();
                        String value = "\n";
                        for (Map.Entry<String, String> sch : repeatSc.entrySet()) {
                            value = value + sch.getValue() + "\n";
                        }
                        Tips(getString(R.string.tips), getString(R.string.device_schedule_overlap, value)).setPositiveButton(R.string.yes, null).create().show();
                    }
                }
            }
        }
    }

    private void saveSchedule(String serial, JSONObject objj) {
        try {
            JSONArray arr = objj.getJSONArray("schedule");
            Map<Integer, ScheduleInfo> schedule = new HashMap<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ScheduleInfo info = new ScheduleInfo();
                info.json = obj.toString();
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
                info.end_s = end.getInt(3);
                schedule.put(Integer.valueOf(info.index), info);
            }
            schedules.put(serial, schedule);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onChooseTime(int hour, int minute, int am, View clickView) {
        if (clickView.getId() == R.id.on) {
            on.setText(getString(R.string.choose_time, df.format(hour), df.format(minute), am == 0 ? "am" : "pm"));
            on.setText(getString(R.string.choose_time, df.format(hour), df.format(minute), am == 0 ? "am" : "pm"));
            start_hour = hour;
            start_minute = minute;
            Log.e(TAG, "Start  " + start_hour + ":" + start_minute);
        } else if (clickView.getId() == R.id.off) {
            off.setText(getString(R.string.choose_time, df.format(hour), df.format(minute), am == 0 ? "am" : "pm"));
            end_hour = hour;
            end_minute = minute;
            Log.e(TAG, "End  " + end_hour + ":" + end_minute);
        }
    }

    private boolean addschedule = false;
    Map<String, DeviceInfo> deviceInfoMap = new ConcurrentHashMap<>();

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                Log.e(TAG, "退出当前Activity  ");
                finish();
                break;
            case R.id.on:
                timePopupWindow.showAtLocation(AddGroupScheduleActivity.this.findViewById(R.id.main), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, start_hour, start_minute, start_hour >= 12 ? 1 : 0, v);
                break;
            case R.id.off:
                timePopupWindow.showAtLocation(AddGroupScheduleActivity.this.findViewById(R.id.main), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, end_hour, end_minute, end_hour >= 12 ? 1 : 0, v);
                break;
            case R.id.weekday:
                dialogMode.show();
                break;
            case R.id.save:
                progressDialog.show();
                schedules.clear();

                scheduleInfo = new ScheduleInfo();
                if (on_switch.getState() == SwitchView.STATE_SWITCH_ON) {//on 打开
                    scheduleInfo.start_s = 1;
                } else {
                    scheduleInfo.start_s = 0;
                }
                if (off_switch.getState() == SwitchView.STATE_SWITCH_ON) {//off打开
                    scheduleInfo.end_s = 1;
                } else {
                    scheduleInfo.end_s = 0;
                }
                if (scheduleInfo.start_s == 0 && scheduleInfo.end_s == 0) {//打开 和 关闭 都禁用
                    finish();
                    return;
                } else if ((start_minute == end_minute && start_hour == end_hour) && scheduleInfo.start_s == 1 && scheduleInfo.end_s == 1) {//时间相同
                    Tips(getString(R.string.tips), getString(R.string.start_end_same)).setPositiveButton(R.string.yes, null).create().show();
                    return;
                }
                if ((start_hour + start_minute / 60.0) > (end_hour + end_minute / 60.0) && scheduleInfo.start_s == 1 && scheduleInfo.end_s == 1) {
                    Log.e(TAG, " 开始时间大于结束时间................................. ");
                    scheduleInfo.end_w = calculateEnd();
                } else {
                    Log.e(TAG, " 开始时间小于结束时间................................. ");
                    scheduleInfo.end_w = calculateStart();
                }

                /**
                 * {
                 *  "devices":"18FE349EFCB2","payload":{
                 *      "schedule":[
                 *      {
                 *          "index":5,"start":[10,43,2,1],"end":[10,48,1,1],"duration":5,"repeat":0,"enable":1,"running":0,"settime":1456710167
                 *          }
                 *          ],"timestamp":1456710273}}
                 *
                 */
                Log.e(TAG, "E_H:" + end_hour + "   E_H:" + end_minute);
                Log.e(TAG, "S_H:" + start_hour + "   E_H:" + start_minute);

                scheduleInfo.start_w = calculateStart();
                scheduleInfo.start_h = start_hour;
                scheduleInfo.start_m = start_minute;

                scheduleInfo.end_h = end_hour;
                scheduleInfo.end_m = end_minute;
                scheduleInfo.repeat = repeat.getState() == 1 ? 0 : 1;
                scheduleInfo.enable = 1;
                if ((end_hour + end_minute / 60.0) > (start_hour + start_minute / 60.0)) {
                    if (end_minute >= start_minute) {
                        scheduleInfo.duration = (end_hour - start_hour) * 60 + (end_minute - start_minute);
                    } else {
                        scheduleInfo.duration = (end_hour - start_hour - 1) * 60 + (end_minute - start_minute + 60);
                    }
                } else {
                    if (end_minute >= start_minute) {
                        scheduleInfo.duration = (end_hour - start_hour + 24) * 60 + (end_minute - start_minute);
                    } else {
                        scheduleInfo.duration = (end_hour - start_hour + 24 - 1) * 60 + (end_minute - start_minute + 60);
                    }
                }

                /**
                 * 1、查找这个组中存在多少设备
                 * 2、请求组中设备的schedule
                 * 3、void handler中得到数据 若大于10条提示不能添加
                 * 4、验证设备的schedule与设置的schedule是否重叠，重叠的话保存
                 * 5、向组中的每个设备发送schedule
                 * 6、自动 finish()
                 */

                Map<String, DeviceInfo> devices = mGroupDevices.get(groupId).getDevices();
                for (DeviceInfo d : devices.values()) {
                    deviceInfoMap.put(d.getSerial(), d);
                }
                addschedule = true;

                getGDeviceSchedule(groupId);
                if (devices.size() > 0) {
                    handler.postDelayed(delayRunnable, 8 * 1000);
                }
                break;
        }
    }

    Handler handler = new Handler();

    Runnable delayRunnable = new Runnable() {
        @Override
        public void run() {
            progressDialog.dismiss();
            handler.removeCallbacks(delayRunnable);
            if (addschedule) {
                Tips(getString(R.string.tips), getString(R.string.group_offonline)).setPositiveButton(R.string.yes, null).create().show();
            } else {
                mDeviceStore.addGroupSchedule(scheduleInfo, groupId);
                finish();
            }
        }
    };


    SwitchView.OnStateChangedListener stateChangedListener = new SwitchView.OnStateChangedListener() {
        @Override
        public void toggleToOn(SwitchView view) {
            view.setState(true);
            switch (view.getId()) {
                case R.id.off_switch:
                    off.setClickable(true);
                    off.setTextColor(getResources().getColor(R.color.black));
                    break;
                case R.id.on_switch:
                    on.setClickable(true);
                    on.setTextColor(getResources().getColor(R.color.black));
                    break;
            }
        }

        @Override
        public void toggleToOff(SwitchView view) {
            view.setState(false);
            switch (view.getId()) {
                case R.id.off_switch:
                    off.setTextColor(getResources().getColor(R.color.gray));
                    break;
                case R.id.on_switch:
                    on.setTextColor(getResources().getColor(R.color.gray));
                    break;
            }
        }
    };


    private void initFindViewById() {
        back = (ImageView) findViewById(R.id.back);
        back.setOnClickListener(this);
        off_switch = (SwitchView) findViewById(R.id.off_switch);
        on_switch = (SwitchView) findViewById(R.id.on_switch);
        repeat = (SwitchView) findViewById(R.id.repeat);
        off_switch.setOnStateChangedListener(stateChangedListener);
        on_switch.setOnStateChangedListener(stateChangedListener);

        off_switch.setState(true);
        on_switch.setState(true);

        on = (TextView) findViewById(R.id.on);
        off = (TextView) findViewById(R.id.off);
        save = (TextView) findViewById(R.id.save);
        weekday = (TextView) findViewById(R.id.weekday);
        weeks = (TextView) findViewById(R.id.weeks);
        on.setOnClickListener(this);
        off.setOnClickListener(this);
        save.setOnClickListener(this);
        weekday.setOnClickListener(this);
        currweek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = 0; i < 7; i++) {
            if (currweek == i) {
                modeInfos.add(new DialogModeInfo(i, weeksFullName[i], true));
            } else {
                modeInfos.add(new DialogModeInfo(i, weeksFullName[i], false));
            }
        }
        dialogMode = new DialogMode(AddGroupScheduleActivity.this, R.style.GroupDialog, getString(R.string.select_weekday));
        dialogMode.setDialogModeListener(dialogListener, modeInfos);
        setWeek(modeInfos);
    }

    private DialogMode.DialogClickListener dialogListener = new DialogMode.DialogClickListener() {
        @Override
        public void onClick(DialogMode context, List<DialogModeInfo> dialogs) {
            setWeek(dialogs);
        }
    };

    private int calculateStart() {
        StringBuffer sb = new StringBuffer();
        for (int i = modeInfos.size() - 1; i >= 0; i--) {
            if (modeInfos.get(i).isState()) {
                sb.append(1);
            } else {
                sb.append(0);
            }
        }
        return Integer.parseInt(sb.toString(), 2);
    }


    private int calculateEnd() {
        StringBuffer sb = new StringBuffer();
        for (int i = modeInfos.size() - 1; i >= 0; i--) {
            if (i == 0) {
                if (modeInfos.get(modeInfos.size() - 1).isState()) {
                    sb.append(1);
                } else {
                    sb.append(0);
                }
            } else if (modeInfos.get(i - 1).isState()) {
                sb.append(1);
            } else {
                sb.append(0);
            }
        }
        return Integer.parseInt(sb.toString(), 2);
    }

    private void setWeek(List<DialogModeInfo> dialogs) {
        String data = "";
        boolean brek = true;
        for (int i = 0; i < dialogs.size(); i++) {
            if (!brek || !dialogs.get(i).isState()) {
                brek = false;
            }
            if (dialogs.get(i).isState()) {
                data = data + " " + dialogs.get(i).getItem_title().toString().substring(0, 3);
            }
        }
        if (brek) {
            weeks.setText(getString(R.string.weekdays));
        } else {
            weeks.setText(data);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        start_hour = calendar.get(Calendar.HOUR_OF_DAY);
        start_minute = calendar.get(Calendar.MINUTE);
        end_hour = calendar.get(Calendar.HOUR_OF_DAY);
        end_minute = calendar.get(Calendar.MINUTE);
        on.setText(getString(R.string.choose_time, df.format(calendar.get(Calendar.HOUR_OF_DAY)), df.format(calendar.get(Calendar.MINUTE)), calendar.get(Calendar.AM_PM) == 0 ? "am" : "pm"));
        off.setText(getString(R.string.choose_time, df.format(calendar.get(Calendar.HOUR_OF_DAY)), df.format(calendar.get(Calendar.MINUTE)), calendar.get(Calendar.AM_PM) == 0 ? "am" : "pm"));

        getGDeviceSchedule(groupId);
    }

    public CustomDialog.Builder Tips(String title, String msg) {
        ibuilder = new CustomDialog.Builder(AddGroupScheduleActivity.this);
        ibuilder.setTitle(title);
        ibuilder.setMessage(msg);
        return ibuilder;
    }

    private CustomDialog.Builder ibuilder = null;
}