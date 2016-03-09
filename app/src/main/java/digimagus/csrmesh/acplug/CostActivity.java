package digimagus.csrmesh.acplug;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.digimagus.aclibrary.HTTPManagementAPI;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * 电费单价
 */
public class CostActivity extends BaseActivity implements View.OnClickListener {
    private TextView cancel, save;

    private ExecutorService pool = Executors.newSingleThreadExecutor();
    private DeviceInfo deviceInfo;
    private EditText countryNO, cost;
    private String serial;

    @Override
    protected void handler(Message message) {
        Log.e(TAG, "udp: " + message.obj);
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_cost);
        serial = getIntent().getStringExtra("serial");
        initFindViewById();
        progressDialog = new ProgressBarDialog(this).createDialog(this);
        progressDialog.setMessage(getString(R.string.positioning));
    }

    private void initFindViewById() {
        countryNO = (EditText) findViewById(R.id.countryNO);
        cost = (EditText) findViewById(R.id.cost);
        cancel = (TextView) findViewById(R.id.cancel);
        save = (TextView) findViewById(R.id.save);
        cancel.setOnClickListener(this);
        save.setOnClickListener(this);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HTTPManagementAPI.PHONE_LOCATE_SUCCESS:
                    progressDialog.dismiss();
                    String[] arr = String.valueOf(msg.obj).split(",");
                    countryNO.setText(arr[0] + " " + arr[2]);
                    break;
                case HTTPManagementAPI.PHONE_LOCATE_FAILED:
                    progressDialog.dismiss();
                    break;
            }
        }
    };

    Runnable locateRunnable = new Runnable() {
        @Override
        public void run() {
            HTTPManagementAPI.getInstance().startLocate(CostActivity.this, handler);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        deviceInfo = devices.get(serial);
        if (deviceInfo.getCountryNO() == null || "".equals(deviceInfo.getCountryNO())) {
            progressDialog.show();
            pool.execute(locateRunnable);
        }
        countryNO.setText(deviceInfo.getCountryNO());
        cost.setText(String.valueOf(deviceInfo.getPrice()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                finish();
                break;
            case R.id.save:
                try {
                    Double price = Double.parseDouble(cost.getText().toString());

                    devices.get(serial).setCountryNO(countryNO.getText().toString());
                    devices.get(serial).setPrice(price);
                    mDeviceStore.updateDevice(serial, devices.get(serial));
                    finish();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Tips(getString(R.string.coat_illegal)).setPositiveButton(R.string.yes, null).create().show();
                }
                break;
        }
    }

    private CustomDialog.Builder ibuilder;

    private CustomDialog.Builder Tips(String msg) {
        ibuilder = new CustomDialog.Builder(this);
        ibuilder.setTitle(R.string.tips);
        ibuilder.setMessage(msg);
        return ibuilder;
    }

    private ProgressBarDialog progressDialog;
}
