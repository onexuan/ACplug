package digimagus.csrmesh.acplug;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import digimagus.csrmesh.adapter.HideDeviceAdapter;
import digimagus.csrmesh.entities.HideDeviceInfo;

/**
 * 显示隐藏设备
 */
public class HideDeviceActivity extends BaseActivity{
    private final static String TAG = "HideDeviceActivity";


    private List<HideDeviceInfo> hides = new ArrayList<>();
    private boolean allselect = false;

    private HideDeviceAdapter hideAdapter;
    private ListView hide_devices;
    private TextView select_all, done;

    @Override
    protected void handler(Message message) {
        Log.e(TAG, "udp: " + message.obj);
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_hidedevice);
        initFindViewById();
    }

    private void initFindViewById() {
        hide_devices = (ListView) findViewById(R.id.hide_devices);
        select_all = (TextView) findViewById(R.id.select_all);
        done = (TextView) findViewById(R.id.done);
        for (HideDeviceInfo info : hideDevices.values()) {
            hides.add(info);
        }
        hideAdapter = new HideDeviceAdapter(this, hides);
        hide_devices.setAdapter(hideAdapter);
        hide_devices.setOnItemClickListener(itemClickListener);
        select_all.setOnClickListener(clickListener);
        done.setOnClickListener(clickListener);
    }


    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            hides.get(position).choose = hides.get(position).choose ? false : true;
            hideAdapter.notifyDataSetChanged();
            allselect = true;
            for (HideDeviceInfo info : hides) {
                if (!info.choose) {
                    allselect = false;
                    select_all.setText(R.string.select_all);
                    break;
                }
            }
            if (allselect) {
                select_all.setText(R.string.unselect_all);
            }
        }
    };

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.done:
                    for (HideDeviceInfo hide : hides) {
                        if (hide.choose) {
                            addShowDevice(hide);
                        }
                    }
                    finish();
                    break;
                case R.id.select_all:
                    if(!hides.isEmpty()){
                        if (allselect) {
                            for (HideDeviceInfo info : hides) {
                                info.choose = false;
                            }
                            allselect = false;
                            select_all.setText(R.string.select_all);
                        } else {
                            for (HideDeviceInfo info : hides) {
                                info.choose = true;
                            }
                            allselect = true;
                            select_all.setText(R.string.unselect_all);
                        }
                        hideAdapter.notifyDataSetChanged();
                    }

                    break;
            }
        }
    };
}
