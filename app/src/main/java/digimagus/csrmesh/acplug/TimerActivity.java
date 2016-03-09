package digimagus.csrmesh.acplug;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import digimagus.csrmesh.view.PercentRelativeLayout;
import digimagus.csrmesh.view.ProgressBarDialog;
import digimagus.csrmesh.view.SwitchView;

/**
 * 定时开关
 */
public class TimerActivity extends BaseActivity implements View.OnClickListener {
    private final static String TAG = "TimerActivity";
    private NumberPicker hour, minute;
    private ImageView back;
    private View top, bottom;
    private TextView start, pause, remaining;
    private SwitchView action;
    private PercentRelativeLayout choose;
    private String serial;
    private ProgressBarDialog progressDialog;

    String[] minutes = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40",
            "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59"};

    @Override
    protected void handler(Message msg) {
        Log.e(TAG, msg.what + " handler:  " + msg.obj);
        try {
            JSONObject obj = new JSONObject(String.valueOf(msg.obj));
            switch (msg.what) {
                case DEVICE_BACK_COUNTDOWN:
                    if (obj.has("topic")) {
                        obj = obj.getJSONObject("data");
                    }
                    obj = obj.getJSONObject("payload");
                    obj = obj.getJSONObject("countdown");
                    act = obj.getInt("action");
                    remain = obj.getInt("remaining");
                    time = obj.getInt("timeleft");
                    enable = obj.getInt("enable");
                    if (remain > 0) {
                        remaining.setVisibility(View.VISIBLE);
                        choose.setVisibility(View.GONE);
                        int h = remain / 60;
                        int m = remain % 60;
                        remaining.setText(Html.fromHtml(getString(R.string.remaining, String.valueOf(h), String.valueOf(m))));


                        Log.e(TAG, "state:  " + action.getState());
                        if ((act == 1 && action.getState() == 4) || (act == 0 && action.getState() == 1)) {
                            action.setState(act == 1 ? false : true);
                        }
                        Log.e(TAG, "state:  " + action.getState());

                        start.setText(getString(R.string.cancel));

                        action.setEnabled(false);

                        if (enable == 1) {//启用
                            pause.setText(getString(R.string.Pause));
                        } else if (enable == 2) {//暂停
                            pause.setText(getString(R.string.Continue));
                        } else if (enable == 0) {//删除

                        }
                        pause.setEnabled(true);
                        pause.setTextColor(getResources().getColor(R.drawable.change_color));
                    } else {
                        remaining.setVisibility(View.GONE);
                        choose.setVisibility(View.VISIBLE);

                        start.setText(getString(R.string.Start));
                        action.setEnabled(true);
                        pause.setText(getString(R.string.Pause));
                        pause.setEnabled(false);
                        pause.setTextColor(getResources().getColor(R.color.gray));
                    }
                    break;
                case PHONE_SET_PARAMEETER_RESULT: {
                    boolean state = obj.getInt("result") == 0 ? true : false;
                    String serial = obj.getString("serial");

                    getDeviceTimer(serial);

                    handler.removeCallbacks(getTimerRunnable);
                    progressDialog.dismiss();
                    if (!pause_e && state && serial.equals(this.serial)) {//设置timer信息 是否成功
                        String txtValue = start.getText().toString();
                        if (getString(R.string.Start).equals(txtValue)) {
                            int h = hour.getValue();
                            int m = minute.getValue();
                            remaining.setVisibility(View.VISIBLE);
                            choose.setVisibility(View.GONE);
                            remaining.setText(Html.fromHtml(getString(R.string.remaining, String.valueOf(h), String.valueOf(m))));
                            start.setText(getString(R.string.cancel));
                            action.setEnabled(false);

                            pause.setEnabled(true);
                            pause.setTextColor(getResources().getColor(R.drawable.change_color));
                        } else if (getString(R.string.cancel).equals(txtValue)) {
                            remaining.setVisibility(View.GONE);
                            choose.setVisibility(View.VISIBLE);
                            start.setText(getString(R.string.Start));
                            action.setEnabled(true);

                            pause.setText(getString(R.string.Pause));
                            pause.setEnabled(false);
                            pause.setTextColor(getResources().getColor(R.color.gray));

                            hour.setValue(remain / 60);
                            minute.setValue(remain % 60);
                        }
                    } else if (pause_e && state && serial.equals(this.serial)) {
                        String txtValue = pause.getText().toString();
                        if ((getString(R.string.Pause).equals(txtValue))) {
                            pause.setText(getString(R.string.Continue));
                        } else if ((getString(R.string.Continue).equals(txtValue))) {
                            pause.setText(getString(R.string.Pause));
                        }
                    }
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        handler.removeCallbacks(getTimerRunnable);
        progressDialog.dismiss();
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_timer);
        progressDialog = new ProgressBarDialog(this).createDialog(this);
        progressDialog.setMessage("");
        serial = getIntent().getStringExtra("serial");
        initFindViewById();
    }

    private void initFindViewById() {
        back = (ImageView) findViewById(R.id.back);
        hour = (NumberPicker) findViewById(R.id.hour);
        minute = (NumberPicker) findViewById(R.id.minute);
        hour.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minute.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        top = findViewById(R.id.top);
        bottom = findViewById(R.id.bottom);
        remaining = (TextView) findViewById(R.id.remaining);
        choose = (PercentRelativeLayout) findViewById(R.id.choose);

        start = (TextView) findViewById(R.id.start);
        pause = (TextView) findViewById(R.id.pause);
        hour.setDisplayedValues(minutes);
        minute.setDisplayedValues(minutes);
        action = (SwitchView) findViewById(R.id.action);
        action.setSwitchViewType(SwitchView.SwitchViewType.SwitchViewTypeGray);

        hour.setMaxValue(23);
        hour.setMinValue(0);
        minute.setMinValue(0);
        minute.setMaxValue(59);
        back.setOnClickListener(this);
        start.setOnClickListener(this);
        pause.setOnClickListener(this);

        setNumberPickerDividerColor(hour);
        setNumberPickerDividerColor(minute);
        /**
         minute.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
         int height = minute.getMeasuredHeight();
         ViewGroup.MarginLayoutParams margintop = new ViewGroup.MarginLayoutParams(top.getLayoutParams());
         margintop.setMargins(0, height / 3, margintop.width, height / 3 + margintop.height);
         RelativeLayout.LayoutParams layoutParamstop = new RelativeLayout.LayoutParams(margintop);
         top.setLayoutParams(layoutParamstop);

         ViewGroup.MarginLayoutParams marginbottom = new ViewGroup.MarginLayoutParams(bottom.getLayoutParams());
         marginbottom.setMargins(0, height * 2 / 3, marginbottom.width, height * 2 / 3 + marginbottom.height);
         RelativeLayout.LayoutParams layoutParamsbottom = new RelativeLayout.LayoutParams(marginbottom);
         bottom.setLayoutParams(layoutParamsbottom);*/
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (devices.get(serial) != null && !devices.get(serial).online) {
            Toast.makeText(TimerActivity.this, getString(R.string.device_not_online), Toast.LENGTH_SHORT).show();
        } else {
            minute.setValue(1);
            progressDialog.show();
            getDeviceTimer(serial);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        progressDialog.dismiss();
    }

    private void setNumberPickerDividerColor(NumberPicker numberPicker) {
        NumberPicker picker = numberPicker;
        Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (Field pf : pickerFields) {
            if (pf.getName().equals("mSelectionDivider")) {
                Log.e(TAG, " mSelectionDivider ");
                try {//设置分割线的颜色值
                    pf.setAccessible(true);
                    pf.set(picker, new ColorDrawable(this.getResources().getColor(R.color.transparent)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            if (pf.getName().equals("mInputText") || pf.getName().equals("mTextSize")) {
                Log.e(TAG, " mInputText ");
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.start://动作  开关  倒计时数(大于0   0取消定时)
                String txt = start.getText().toString();
                if (devices.get(serial) == null || !devices.get(serial).online) {
                    Toast.makeText(TimerActivity.this, getString(R.string.device_not_online), Toast.LENGTH_SHORT).show();
                    return;
                } else if (hour.getValue() == 0 && minute.getValue() == 0 && !getString(R.string.cancel).equals(txt)) {
                    return;
                }
                progressDialog.show();
                if (getString(R.string.Start).equals(txt)) {
                    int toatl = hour.getValue() * 60 + minute.getValue();
                    if (toatl > 0) {
                        StringBuffer timer = new StringBuffer();
                        timer.append("{\"action\":");
                        timer.append(action.getState() == 1 ? 1 : 0);
                        timer.append(",\"timeleft\":");
                        timer.append(toatl);
                        timer.append(",\"enable\":1}");
                        setDeviceTimer(serial, timer.toString());
                    }
                } else if (getString(R.string.cancel).equals(txt)) {
                    StringBuffer timer = new StringBuffer();
                    timer.append("{\"action\":");
                    timer.append(action.getState() == 1 ? 1 : 0);
                    timer.append(",\"timeleft\":");
                    timer.append(1);
                    timer.append(",\"enable\":0}");
                    setDeviceTimer(serial, timer.toString());
                }
                gettimer = true;
                handler.postDelayed(getTimerRunnable, 5000);
                break;
            case R.id.pause://暂停
                if (devices.get(serial) == null || !devices.get(serial).online) {
                    Toast.makeText(TimerActivity.this, getString(R.string.device_not_online), Toast.LENGTH_SHORT).show();
                    return;
                }

                progressDialog.show();
                String txt_pause = pause.getText().toString();

                if (getString(R.string.Pause).equals(txt_pause)) {
                    StringBuffer timer = new StringBuffer();
                    timer.append("{\"action\":");
                    timer.append(act);
                    timer.append(",\"timeleft\":");
                    timer.append(time);
                    timer.append(",\"enable\":2}");
                    setDeviceTimer(serial, timer.toString());
                } else if (getString(R.string.Continue).equals(txt_pause)) {
                    StringBuffer timer = new StringBuffer();
                    timer.append("{\"action\":");
                    timer.append(act);
                    timer.append(",\"timeleft\":");
                    timer.append(time);
                    timer.append(",\"enable\":1}");
                    setDeviceTimer(serial, timer.toString());
                }
                pause_e = true;
                gettimer = true;
                handler.postDelayed(getTimerRunnable, 7000);
                break;
        }
    }

    private int act, remain, time, enable;
    private boolean pause_e = false, gettimer;

    Handler handler = new Handler();

    Runnable getTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (gettimer) {
                getDeviceTimer(serial);
                handler.postDelayed(getTimerRunnable, 5000);
                gettimer = false;
            } else {
                handler.removeCallbacks(getTimerRunnable);
                progressDialog.dismiss();
            }
        }
    };
}
