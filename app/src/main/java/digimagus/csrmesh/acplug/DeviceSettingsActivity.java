package digimagus.csrmesh.acplug;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import digimagus.csrmesh.entities.DeviceInfo;

/**
 * Device Setting界面
 */
public class DeviceSettingsActivity extends BaseActivity implements View.OnClickListener {
    private final static String TAG = "DeviceSettingsActivity";
    private RelativeLayout edit_name, schedule, cost, export, timer, notification;
    private ImageView back;
    private TextView connecting, power, serial;
    private DeviceInfo deviceInfo;
    private Bundle bundle;
    private TextView deviceName, name, firmware_version;

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_devicesettings);
        bundle = getIntent().getBundleExtra("bundle");
        initFindViewById();
    }

    @Override
    protected void handler(Message msg) {
        Log.e(TAG, "udp: " + msg.obj);
        try {
            JSONObject obj = new JSONObject(String.valueOf(msg.obj));
            switch (msg.what) {
                case DEVICE_BACK_VERSION: {
                    if (obj.has("topic")) {
                        obj=obj.getJSONObject("data");
                    }
                    String swversion=obj.getJSONObject("payload").getString("swversion");
                    firmware_version.setText(swversion);
                    devices.get(deviceInfo.getSerial()).swversion=swversion;
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initFindViewById() {
        edit_name = (RelativeLayout) findViewById(R.id.edit_name);
        schedule = (RelativeLayout) findViewById(R.id.schedule);
        cost = (RelativeLayout) findViewById(R.id.cost);
        export = (RelativeLayout) findViewById(R.id.export);
        timer = (RelativeLayout) findViewById(R.id.timer);
        notification = (RelativeLayout) findViewById(R.id.notification);
        serial = (TextView) findViewById(R.id.serial);

        connecting = (TextView) findViewById(R.id.connecting);
        back = (ImageView) findViewById(R.id.back);
        deviceName = (TextView) findViewById(R.id.deviceName);
        name = (TextView) findViewById(R.id.name);
        firmware_version = (TextView) findViewById(R.id.firmware_version);
        power = (TextView) findViewById(R.id.power);
        back.setOnClickListener(this);
        edit_name.setOnClickListener(this);
        schedule.setOnClickListener(this);
        cost.setOnClickListener(this);
        export.setOnClickListener(this);
        timer.setOnClickListener(this);
        notification.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_name:
                Intent edit_intent = new Intent(this, EditNameActivity.class);
                Bundle bundle_ = new Bundle();
                edit_intent.putExtra("bundle", bundle_);
                bundle_.putString("editType", "editDevice");
                bundle_.putInt("id", deviceInfo.getId());
                bundle_.putString("name", deviceInfo.getName());
                bundle_.putString("serial", deviceInfo.getSerial());
                startActivity(edit_intent);
                break;
            case R.id.schedule:
                Intent schedule_intent = new Intent(this, ScheduleActivity.class);
                schedule_intent.putExtra("bundle", bundle);
                startActivity(schedule_intent);
                break;
            case R.id.cost:
                Intent cost_intent = new Intent(this, CostActivity.class);
                cost_intent.putExtra("serial", deviceInfo.getSerial());
                startActivity(cost_intent);
                break;
            case R.id.export:
                Intent export_intent = new Intent(this, ExportActivity.class);
                export_intent.putExtra("mPhoneInfo",phoneInfo);
                export_intent.putExtra("serial", deviceInfo.getSerial());
                export_intent.putExtra("signal",wiFiManagementAPI.getWiFiInfo().getRssi());
                startActivity(export_intent);
                break;
            case R.id.reset:
                Intent reset_intent = new Intent(this, ResetActivity.class);
                reset_intent.putExtra("serial", deviceInfo.getSerial());
                startActivity(reset_intent);
                break;
            case R.id.timer:
                Intent timer = new Intent(this, TimerActivity.class);
                timer.putExtra("serial", deviceInfo.getSerial());
                startActivity(timer);
                break;
            case R.id.notification:
                Intent notifica = new Intent(this, NotificationActivity.class);
                notifica.putExtra("mPhoneInfo",phoneInfo);
                notifica.putExtra("mDevuuid",deviceInfo.getUuid());
                startActivity(notifica);
                break;
            case R.id.back:
                finish();
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        deviceInfo = devices.get(bundle.getString("serial"));
        if(deviceInfo.swversion==null){//如果存在版本号不发送查询消息
            queryDeviceState(deviceInfo, 3);
        }else{
            firmware_version.setText(deviceInfo.swversion);
        }
        deviceName.setText(deviceInfo.getName());
        serial.setText(deviceInfo.getSerial());

        name.setText(deviceInfo.getName() == null ? deviceInfo.getSerial() : deviceInfo.getName());

        power.setText(getString(R.string.power_s, String.valueOf(deviceInfo.getPrice())));
        connecting.setText(getString(deviceInfo.online ? R.string.connected : R.string.disconnected));
    }
}
