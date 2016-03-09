package digimagus.csrmesh.acplug;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.digimagus.aclibrary.MessageService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import digimagus.csrmesh.adapter.SlipDMenuAdapter;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * Group 信息界面
 */
public class GroupSettingActivity extends BaseActivity implements View.OnClickListener, SlipDMenuAdapter.ControlDeviceListener {

    DisplayMetrics dm = new DisplayMetrics();
    private RelativeLayout add_device, edit_group, schedule;
    private ImageView back;
    private TextView name_title, name;
    private List<DeviceInfo> infos = new ArrayList<>();
    private Bundle bundle;
    private SlipDMenuAdapter device_adapter;
    private ListView device_list;


    @Override
    protected void handler(Message msg) {
        try {
            if (msg.what == NOTYFY_UI_CHANGE) {
                Log.e(TAG, "serial: " + msg.obj);
                String serial=String.valueOf(msg.obj);
                updateDeviceStatus(devices.get(serial), devices.get(serial).state, devices.get(serial).remaining);
            } else {
                JSONObject obj = new JSONObject(String.valueOf(msg.obj));
                switch (msg.what){

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_groupsetting);
        bundle = getIntent().getBundleExtra("bundle");
        initFindViewById();
    }

    private void initFindViewById() {
        add_device = (RelativeLayout) findViewById(R.id.add_device);
        back = (ImageView) findViewById(R.id.back);
        edit_group = (RelativeLayout) findViewById(R.id.edit_group);
        name_title = (TextView) findViewById(R.id.name_title);
        name = (TextView) findViewById(R.id.name);
        schedule = (RelativeLayout) findViewById(R.id.schedule);
        device_list = (ListView) findViewById(R.id.device_list);
        edit_group.setOnClickListener(this);
        add_device.setOnClickListener(this);
        back.setOnClickListener(this);
        //schedule.setOnClickListener(this);
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        device_adapter = new SlipDMenuAdapter(this, infos, dm.widthPixels);
        device_adapter.setControlDeviceListener(this);
        device_list.setAdapter(device_adapter);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 01:
                    showProgressDialog(getString(R.string.remove_device, "...")).show();
                    DeviceInfo device = (DeviceInfo) msg.obj;
                    postDelayed(getGroupDevice, 2000);

                    mDeviceStore.removeDeviceGroup(bundle.getInt("id"), device.getSerial());
                    /*移除设备*/
                    mGroupDevices.get(bundle.getInt("id")).getDevices().remove(device.getSerial());
                    infos.remove(device);
                    break;
                case 02:
                    if (progress != null) {
                        progress.dismiss();
                    }
                    //得到组中的设备
                    device_adapter.notifyDataSetChanged(infos);
                    break;
            }
        }
    };

    private ProgressBarDialog progress;

    private ProgressBarDialog showProgressDialog(String msg) {
        progress = new ProgressBarDialog(this).createDialog(this);
        progress.setMessage(msg);
        return progress;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.edit_group:
                Intent edit_intent = new Intent(this, EditNameActivity.class);
                bundle.putString("editType", "editGroup");
                edit_intent.putExtra("bundle", bundle);
                startActivity(edit_intent);
                break;
            case R.id.schedule:
                if (group == null || group.getDevices() == null || group.getDevices().isEmpty() || !group.online) {
                    Tips(getString(R.string.group_no_device)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                } else {
                    Intent schedule_intent = new Intent(this, GroupScheduleActivity.class);
                    schedule_intent.putExtra("groupId", bundle.getInt("id"));
                    startActivity(schedule_intent);
                }
                break;
            case R.id.add_device:
                Intent device_intent = new Intent(this, GroupDeviceActivity.class);
                device_intent.putExtra("bundle", bundle);
                startActivity(device_intent);
                break;
        }
    }

    private CustomDialog.Builder ibuilder;

    private CustomDialog.Builder Tips(String msg) {
        ibuilder = new CustomDialog.Builder(this);
        ibuilder.setTitle(R.string.remove_tips);
        ibuilder.setMessage(msg);
        return ibuilder;
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(getGroupDevice);
        group = mDeviceStore.getGroupById(bundle.getInt("id"));
        name_title.setText(group.getName());
        name.setText(group.getName());

        infos = mDeviceStore.getGroupDeviceById(bundle.getInt("id"));
        device_adapter.notifyDataSetChanged(infos);
    }

    Runnable getGroupDevice = new Runnable() {
        @Override
        public void run() {
            Message message = handler.obtainMessage();
            message.what = 02;
            handler.sendMessage(message);
        }
    };

    public void updateDeviceStatus(DeviceInfo info, int status, int remaining) {
        for (DeviceInfo d : infos) {
            if (info.getSerial().equals(d.getSerial())) {
                d.online = true;
                d.state=status;
                d.remaining = remaining;
                device_adapter.notifyDataSetChanged(infos);
                break;
            }
        }
    }

    private final static String TAG = "GroupSettingActivity";

    @Override
    public void controlDevice(final DeviceInfo device, View v) {
        switch (v.getId()) {
            case R.id.right:
                if (MessageService.CURRENT_NETWORK_TYPE == MessageService.CONN_NETWORK_TYPE_NONE) {
                    Tips(getString(R.string.tips, getString(R.string.connect_to_network))).setPositiveButton(R.string.yes, null).create().show();
                } else {
                    if (device.online) {
                        setDeviceState(device);
                        v.findViewById(R.id.send_msg).setVisibility(View.VISIBLE);
                        ((ImageView) v.findViewById(R.id.device_state)).setImageResource(R.mipmap.icon_activated);
                    } else {//Device is not online
                        Tips(getString(R.string.tips, getString(R.string.device_not_online))).setNegativeButton(R.string.yes, null).create().show();
                    }
                }
                break;
            case R.id.setting:
                Intent intent = new Intent(GroupSettingActivity.this, DeviceSettingsActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("id", device.getId());
                bundle.putString("name", device.getName());
                bundle.putString("serial", device.getSerial());
                bundle.putString("uuid", device.getUuid());
                intent.putExtra("bundle", bundle);
                GroupSettingActivity.this.startActivity(intent);
                break;
            case R.id.delete:
                Tips(getString(R.string.remove_device_group)).setNegativeButton(R.string.no, null).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Message message = handler.obtainMessage();
                        message.obj = device;
                        message.what = 01;
                        handler.sendMessage(message);
                    }
                }).create().show();
                break;
            case R.id.content: {
                Bundle bundle_c = new Bundle();
                bundle_c.putInt("id", device.getId());
                bundle_c.putString("serial", device.getSerial());
                bundle_c.putString("online", device.online ? (device.state == 1 ? "on" : "off") : "Offline");
                bundle_c.putString("name", device.getName());
                bundle_c.putString("uuid", device.getUuid());
                bundle_c.putDouble("power", device.power);

                Intent intent_c = new Intent(GroupSettingActivity.this, SummaryActivity.class);
                intent_c.putExtra("bundle", bundle_c);
                GroupSettingActivity.this.startActivity(intent_c);
                break;
            }

        }
    }

}
