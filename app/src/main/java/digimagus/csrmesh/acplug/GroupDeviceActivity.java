package digimagus.csrmesh.acplug;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * 向组中添加设备
 */
public class GroupDeviceActivity extends BaseActivity implements View.OnClickListener {
    private final static String TAG = "GroupDeviceActivity";
    private ListView device_list;
    private TextView title, all, done;
    private DeviceAdapter deviceAdapter;
    private List<DeviceInfo> deviceInfos = new ArrayList<>();
    private Bundle bundle;
    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_groupdevice);
        bundle = getIntent().getBundleExtra("bundle");
        initFindViewById();
    }

    @Override
    protected void handler(Message message) {
        Log.e(TAG, "udp: " + message.obj);
    }

    private void initFindViewById() {
        title = (TextView) findViewById(R.id.title);
        all = (TextView) findViewById(R.id.all);
        device_list = (ListView) findViewById(R.id.device_list);
        done = (TextView) findViewById(R.id.done);
        all.setOnClickListener(this);
        done.setOnClickListener(this);
        deviceAdapter = new DeviceAdapter(this);
        device_list.setAdapter(deviceAdapter);
        device_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                deviceInfos.get(position).setChoose(deviceInfos.get(position).isChoose() ? false : true);
                deviceAdapter.notifyDataSetChanged();
                allselect = true;
                for (DeviceInfo info : deviceInfos) {
                    if (!info.isChoose()) {
                        allselect = false;
                        all.setText(R.string.select_all);
                        break;
                    }
                }
                if (allselect) {
                    all.setText(R.string.unselect_all);
                }
            }
        });
    }

    private boolean allselect = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.all: {
                if (allselect) {
                    for (DeviceInfo info : deviceInfos) {
                        info.setChoose(false);
                    }
                    all.setText(R.string.select_all);
                } else {
                    for (DeviceInfo info : deviceInfos) {
                        info.setChoose(true);
                    }
                    all.setText(R.string.unselect_all);
                }
                allselect = !allselect;
                deviceAdapter.notifyDataSetChanged();
                break;
            }
            case R.id.done: {
                chooseDevice.clear();
                for (int i = 0; i < deviceInfos.size(); i++) {
                    if (deviceInfos.get(i).isChoose()) {
                        chooseDevice.add(deviceInfos.get(i));
                    }
                }
                if (chooseDevice.size() == 0) {
                    finish();
                } else {
                    for (DeviceInfo info : chooseDevice) {
                        mDeviceStore.updateGroupById(bundle.getInt("id"), info.getSerial());

                        mGroupDevices.get(bundle.getInt("id")).getDevices().put(info.getSerial(),devices.get(info.getSerial()));
                    }
                    chooseDevice.clear();
                    showProgressDialog(GroupDeviceActivity.this).setMessage(getString(R.string.saving_data)).show();
                    handler.postDelayed(saveRunnable, 2000);
                }
                break;
            }
        }
    }

    private ProgressBarDialog progressDialog;

    private ProgressBarDialog showProgressDialog(Context context) {
        if (progressDialog == null) {
            progressDialog = new ProgressBarDialog(context).createDialog(context);
        }
        progressDialog.setMessage(getString(R.string.save_data));
        return progressDialog;
    }

    Handler handler = new Handler();

    Runnable saveRunnable = new Runnable() {
        @Override
        public void run() {
            showProgressDialog(GroupDeviceActivity.this).dismiss();
            finish();
        }
    };


    private List<DeviceInfo> chooseDevice = new ArrayList<>();

    @Override
    protected void onStart() {
        super.onStart();
        deviceInfos.clear();
        title.setText(bundle.getString("name"));
        Map<String,DeviceInfo> chose = mGroupDevices.get(bundle.getInt("id")).getDevices();
        for (DeviceInfo info : devices.values()) {
            if(chose.isEmpty()){
                info.setChoose(false);
                deviceInfos.add(info);
            }else{
                for (DeviceInfo device:chose.values()){
                    if(device.getSerial().equals(info.getSerial())){
                        break;
                    }else{
                        info.setChoose(false);
                        deviceInfos.add(info);
                    }
                }
            }
        }
        deviceAdapter.notifyDataSetChanged();
    }


    private class DeviceAdapter extends BaseAdapter {
        private Context context;

        public DeviceAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return deviceInfos.size();
        }

        @Override
        public Object getItem(int position) {
            return deviceInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            DeviceInfo info = deviceInfos.get(position);
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.item_add, null);
                holder.device_name = (TextView) convertView.findViewById(R.id.device_name);
                holder.device_choose = (TextView) convertView.findViewById(R.id.choose);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.device_name.setText(info.getName() == null ? info.getSerial() : info.getName());
            holder.device_choose.setBackgroundResource(info.isChoose() ? R.drawable.choose : R.drawable.unchoose);
            return convertView;
        }

        class ViewHolder {
            private TextView device_name;
            private TextView device_choose;
        }
    }
}
