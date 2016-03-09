package digimagus.csrmesh.acplug;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import digimagus.csrmesh.acplug.listener.TurnListener;
import digimagus.csrmesh.acplug.util.VerificationSchedule;
import digimagus.csrmesh.entities.DeviceSerializableMap;
import digimagus.csrmesh.entities.DialogModeInfo;
import digimagus.csrmesh.entities.ScheduleInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.DialogMode;
import digimagus.csrmesh.view.ProgressBarDialog;
import digimagus.csrmesh.view.SwitchView;
import digimagus.csrmesh.view.TimeSelectPopupWindow;

/**
 * 定时打开或者定时关闭
 */
public class DeviceScheduleActivity extends BaseActivity implements View.OnClickListener, TurnListener, TimeSelectPopupWindow.ChooseTimeListener {
    private final static String TAG = "DeviceScheduleActivity";
    private ImageView back;
    private TextView on, off, save, weekday, weeks;
    private SwitchView off_switch, on_switch, repeat;
    private TimeSelectPopupWindow timePopupWindow;
    private DialogMode dialogMode;
    private DecimalFormat df = new DecimalFormat("00");
    private Calendar calendar = Calendar.getInstance();
    private String[] weeksFullName = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private List<DialogModeInfo> modeInfos = new ArrayList<>();
    private int currweek;
    private ProgressBarDialog progressDialog;
    private String operationType;
    private String serial;
    private Map<Integer, ScheduleInfo> schedules=new HashMap<>();
    private int index;
    private ScheduleInfo info;
    private boolean sendschedu = false;
    private String schedule;
    private int start_hour, start_minute, end_hour, end_minute;
    private ScheduleInfo scheduleInfo = new ScheduleInfo();
    @Override
    protected void handler(Message msg) {
        Log.e(TAG, msg.what + "schedule : " + msg.obj);
        try {
            JSONObject obj = new JSONObject(String.valueOf(msg.obj));
            switch (msg.what) {
                case PHONE_SET_PARAMEETER_RESULT: {
                    setSuccess(obj.getString("serial"), obj.getInt("result") == 0 ? true : false);
                    break;
                }
                case DEVICE_BACK_SCHEDULE: {
                    String serial;
                    if (obj.has("topic")) {
                        obj = obj.getJSONObject("data");
                        serial = mUUIDSERIAL.get(obj.getString("fromUuid"));
                        sendschedu = true;
                    } else {
                        serial = obj.getString("devices");
                    }
                    obj = obj.getJSONObject("payload");
                    Object ob = new JsonParser().parse(obj.getString("schedule"));
                    if (ob instanceof JSONObject) {
                        obj = obj.getJSONObject("schedule");
                        setSuccess(serial, obj.getInt("result") == 0 ? true : false);
                    } else {
                        Log.e(TAG, "obj  " + obj.getJSONArray("schedule"));
                        schedule(serial, obj);
                    }
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    Runnable finishRunnable=new Runnable() {
        @Override
        public void run() {
            DeviceScheduleActivity.this.finish();
        }
    };

    public void schedule(String serial, JSONObject schedule) {
        schedules.clear();
        if (serial.equals(this.serial) && schedule != null) {
            try {
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
                    schedules.put(info.index, info);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (progressDialog != null) {
            progressDialog.dismiss();
        }

        if ("add".equals(operationType)) {
            for (int i = 0; i < 10; i++) {
                if (schedules.get(Integer.valueOf(i)) == null) {
                    index = i;
                    break;
                }
            }
        } else if ("update".equals(operationType)) {
            info = (ScheduleInfo) getIntent().getSerializableExtra("info");
            index = info.index;
        }
        sendMsg = false;
        List<String> repeatSchedule = VerificationSchedule.getInstance().verification(this, operationType, scheduleInfo, schedules);
        StringBuffer sb = new StringBuffer();
        if (repeatSchedule.isEmpty()) {
            String start_json = "\"start\":[" + scheduleInfo.start_h + "," + scheduleInfo.start_m + "," + scheduleInfo.start_w + "," + scheduleInfo.start_s + "]";
            String end_json = "\"end\":[" + scheduleInfo.end_h + "," + scheduleInfo.end_m + "," + scheduleInfo.end_w + "," + scheduleInfo.end_s + "]";
            sb.append("{");
            sb.append("\"schedule\":{\"index\":");
            sb.append(index);
            sb.append(",");
            sb.append(start_json);
            sb.append(",");
            sb.append(end_json);
            sb.append(",\"duration\":");
            sb.append(scheduleInfo.duration);
            sb.append(",\"repeat\":");
            sb.append(repeat.getState() == 1 ? 0 : 1);
            sb.append(",\"enable\":");
            sb.append(1);
            sb.append("}");
            sb.append("}");
            this.schedule = sb.toString();
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog.setMessage(getString(R.string.set_timer)).show();
            }
            setDeviceSystemTime(serial);
            handler.postDelayed(delayedRunnable, 2 * 3000);

            handler.postDelayed(finishRunnable, 6000);
        } else {
            StringBuffer tips = new StringBuffer();
            tips.append("\n");
            for (String s : repeatSchedule) {
                tips.append(s);
                tips.append(",\n");
            }
            Tips(getString(R.string.tips), getString(R.string.device_schedule_overlap, tips.toString())).setPositiveButton(R.string.yes, null).create().show();
        }
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_turn);
        progressDialog = new ProgressBarDialog(this).createDialog(this);
        operationType = getIntent().getStringExtra("type");
        serial = getIntent().getStringExtra("serial");
        if("update".equals(operationType)){
            info = (ScheduleInfo) getIntent().getSerializableExtra("info");
            index = info.index;
        }
        initFindViewById();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (info != null) {
            start_hour = info.start_h;
            start_minute = info.start_m;
            end_hour = info.end_h;
            end_minute = info.end_m;
            if (info.repeat == 0) {
                repeat.setState(false);
            } else {
                repeat.setState(true);
            }
            if (info.start_s == 1 && on_switch.getState() == 1) {
                on_switch.setState(true);
            } else if (info.start_s == 0 && (on_switch.getState() == 2 || on_switch.getState() == 4)) {
                on_switch.setState(false);
            }
            if (info.end_s == 1 && off_switch.getState() == 1) {
                off_switch.setState(true);
            } else if (info.end_s == 0 && (off_switch.getState() == 2 || off_switch.getState() == 4)) {
                off_switch.setState(false);
            }
            on.setTextColor(getResources().getColor(info.start_s == 1 ? R.color.black : R.color.gray));
            off.setTextColor(getResources().getColor(info.end_s == 1 ? R.color.black : R.color.gray));
            on.setText(getString(R.string.choose_time, df.format(start_hour), df.format(start_minute), start_hour <= 11 ? "am" : "pm"));
            off.setText(getString(R.string.choose_time, df.format(end_hour), df.format(end_minute), end_hour <= 11 ? "am" : "pm"));
        } else {
            start_hour = calendar.get(Calendar.HOUR_OF_DAY);
            start_minute = calendar.get(Calendar.MINUTE);
            end_hour = calendar.get(Calendar.HOUR_OF_DAY);
            end_minute = calendar.get(Calendar.MINUTE);
            on.setText(getString(R.string.choose_time, df.format(calendar.get(Calendar.HOUR_OF_DAY)), df.format(calendar.get(Calendar.MINUTE)), calendar.get(Calendar.AM_PM) == 0 ? "am" : "pm"));
            off.setText(getString(R.string.choose_time, df.format(calendar.get(Calendar.HOUR_OF_DAY)), df.format(calendar.get(Calendar.MINUTE)), calendar.get(Calendar.AM_PM) == 0 ? "am" : "pm"));
        }
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
    public void onChooseTime(int hour, int minute, int am, View clickView) {
        if (clickView.getId() == R.id.on) {
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
                    off.setClickable(false);
                    break;
                case R.id.on_switch:
                    on.setTextColor(getResources().getColor(R.color.gray));
                    on.setClickable(false);
                    break;
            }
        }
    };

    private void initFindViewById() {
        back = (ImageView) findViewById(R.id.back);
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
        back.setOnClickListener(this);
        timePopupWindow = new TimeSelectPopupWindow(DeviceScheduleActivity.this);
        on.setOnClickListener(this);
        off.setOnClickListener(this);
        save.setOnClickListener(this);
        weekday.setOnClickListener(this);
        if (info == null) {
            currweek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            for (int i = 0; i < 7; i++) {
                if (i == currweek) {
                    myweeks[i] = 1;
                } else {
                    myweeks[i] = 0;
                }
            }
        } else {
            getWeek(info.start_w);
        }
        for (int i = 0; i < 7; i++) {
            if (myweeks[i] == 1) {
                modeInfos.add(new DialogModeInfo(i, weeksFullName[i], true));
            } else {
                modeInfos.add(new DialogModeInfo(i, weeksFullName[i], false));
            }
        }
        dialogMode = new DialogMode(DeviceScheduleActivity.this, R.style.GroupDialog, getString(R.string.select_weekday));
        dialogMode.setDialogModeListener(dialogListener, modeInfos);
        setWeek(modeInfos);
    }


    private void getWeek(int week) {
        String data = Integer.toBinaryString(week);
        int len = data.length();
        String add = "";
        for (int i = 0; i < 7 - len; i++) {
            add = add + "0";
        }
        data = add + data;
        String[] weeks = data.trim().split("");
        for (int i = weeks.length; i > 1; i--) {
            this.myweeks[8 - i] = weeks[i - 1].equals("1") ? 1 : 0;
        }
    }

    private int[] myweeks = new int[7];

    private DialogMode.DialogClickListener dialogListener = new DialogMode.DialogClickListener() {
        @Override
        public void onClick(DialogMode context, List<DialogModeInfo> dialogs) {
            setWeek(dialogs);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.on:
                timePopupWindow.showAtLocation(DeviceScheduleActivity.this.findViewById(R.id.main), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, start_hour, start_minute, start_hour >= 12 ? 1 : 0, v);
                break;
            case R.id.off:
                timePopupWindow.showAtLocation(DeviceScheduleActivity.this.findViewById(R.id.main), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, end_hour, end_minute, end_hour >= 12 ? 1 : 0, v);
                break;
            case R.id.save:
                scheduleInfo.index = index;
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
                    scheduleInfo.end_w = calculateEnd();
                } else {
                    scheduleInfo.end_w = calculateStart();
                }
                scheduleInfo.start_w = calculateStart();
                scheduleInfo.start_h = start_hour;
                scheduleInfo.start_m = start_minute;

                scheduleInfo.end_h = end_hour;
                scheduleInfo.end_m = end_minute;
                scheduleInfo.repeat = repeat.getState() == 1 ? 0 : 1;
                scheduleInfo.enable = (info == null ? 1 : info.enable);
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
                progressDialog.setMessage(getString(R.string.query_schedule)).show();//查询schedule
                queryDeviceState(devices.get(serial), 4);
                break;
            case R.id.weekday:
                dialogMode.show();
                break;
        }
    }

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

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(finishRunnable);
        handler.removeCallbacks(delayedRunnable);
    }


    private CustomDialog.Builder ibuilder = null;

    public CustomDialog.Builder Tips(String title, String msg) {
        ibuilder = new CustomDialog.Builder(DeviceScheduleActivity.this);
        ibuilder.setTitle(title);
        ibuilder.setMessage(msg);
        return ibuilder;
    }

    private boolean sendMsg = true;

    public void setSuccess(String serial, boolean status) {
        Log.e(TAG, "setSuccess ： " + sendMsg + "  " + serial.equals(this.serial));
        if (!serial.equals(this.serial) || sendMsg) {
            return;
        }
        if (sendschedu) {//设置定时计划返回
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if (status) {//定时计划设置成功
                finish();
            } else {//定时计划设置失败
                Tips(getString(R.string.timing_plan_settings), getString(R.string.timing_plan_failed)).setPositiveButton(R.string.yes, null).create().show();
            }
        } else {//设置时间返回
            if (status && schedule != null) {//时间设置成功  -- 开始设置定时计划
                Log.e(TAG, "时间设置成功： " + schedule);
                progressDialog.setMessage(getString(R.string.set_timer_plan));
                sendschedu = true;
                setSchedule(serial, schedule);
            } else {//时间设置失败
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                Tips(getString(R.string.tips), getString(R.string.timeout)).setPositiveButton(R.string.yes, null).create().show();
            }
            handler.removeCallbacks(delayedRunnable);
        }
    }

    Handler handler = new Handler();
    Runnable delayedRunnable = new Runnable() {
        @Override
        public void run() {
            sendschedu = true;
            setSchedule(serial, schedule);
        }
    };

}