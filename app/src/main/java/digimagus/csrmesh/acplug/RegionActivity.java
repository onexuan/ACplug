package digimagus.csrmesh.acplug;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.digimagus.aclibrary.HTTPManagementAPI;
import com.digimagus.aclibrary.MessageService;

import digimagus.csrmesh.acplug.util.Utils;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.EditTextWithDel;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * 地区
 */
public class RegionActivity extends Activity implements View.OnClickListener {
    private final static String TAG = "RegionActivity";
    private HTTPManagementAPI httpManagementAPI;
    private TextView save;
    private EditTextWithDel mylocation;
    private EditTextWithDel device_name;
    private DeviceStore deviceStore;
    private String name, serial;
    private DeviceInfo deviceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_region);
        deviceStore = DeviceStore.getInstance(RegionActivity.this);
        httpManagementAPI = new HTTPManagementAPI();
        name = getIntent().getStringExtra("name");
        serial = getIntent().getStringExtra("serial");

        barDialog = new ProgressBarDialog(this).createDialog(this);
        initFindViewById();
    }

    private void initFindViewById() {
        device_name = (EditTextWithDel) findViewById(R.id.device_name);
        save = (TextView) findViewById(R.id.save);
        mylocation = (EditTextWithDel) findViewById(R.id.location);
        save.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        device_name.setText(name);
        barDialog.setMessage(getString(R.string.mobile_positioning)).show();
        httpManagementAPI.startLocate(this, handler);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HTTPManagementAPI.PHONE_LOCATE_FAILED:
                    barDialog.dismiss();
                    break;
                case HTTPManagementAPI.PHONE_LOCATE_SUCCESS: {
                    String[] loc = String.valueOf(msg.obj).split(",");
                    mylocation.setText(loc[1]);
                    barDialog.dismiss();
                    countryNo = loc[0];
                    location = loc[1];
                    break;
                }
            }
        }
    };

    private String location, countryNo;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save:
                final String name = device_name.getText().toString();
                if (name.length() <= 0) {
                    tips(getString(R.string.not_empty)).setPositiveButton(R.string.yes, null).create().show();
                } else if (Utils.sqlInjection(name)) {
                    tips(getString(R.string.contain_sql_keyword)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                } else if (Utils.sqlInjection(name) || (name.length() < 2 || name.length() > 18)) {
                    tips(getString(R.string.enter_device_message)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                } else {
                    deviceInfo = deviceStore.getDeviceInfo(serial);
                    Log.e(TAG, "对象恒等于null     " + (deviceInfo == null));
                    if (deviceInfo == null) {
                        deviceInfo = new DeviceInfo();
                    }
                    Log.e(TAG, "     " + deviceInfo.getId() + " -->      " + deviceInfo.getSerial());
                    deviceInfo.setName(name);
                    deviceInfo.setMac(getIntent().getStringExtra("mac"));
                    deviceInfo.setSerial(serial);
                    deviceInfo.setLocation(mylocation.getText().toString());
                    deviceInfo.setCountryNO(countryNo);
                    deviceStore.addDeviceInfo(deviceInfo);
                    barDialog.setMessage(getString(R.string.saving_data)).show();
                    handler.postDelayed(saveData, MessageService.SAVE_DATA);

                }
                break;
        }
    }

    Runnable saveData = new Runnable() {
        @Override
        public void run() {
            barDialog.dismiss();
            if (getIntent().getBooleanExtra("first_time", true)) {
                Intent intent = new Intent(RegionActivity.this, MainActivity.class);
                startActivity(intent);
            }
            finish();
        }
    };

    private ProgressBarDialog barDialog;
    private CustomDialog.Builder ibuilder;

    private CustomDialog.Builder tips(String msg) {
        ibuilder = new CustomDialog.Builder(RegionActivity.this);
        ibuilder.setTitle(R.string.tips);
        ibuilder.setMessage(msg);
        return ibuilder;
    }
}
