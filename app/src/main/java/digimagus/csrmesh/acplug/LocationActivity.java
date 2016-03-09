package digimagus.csrmesh.acplug;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import com.digimagus.aclibrary.HTTPManagementAPI;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import digimagus.csrmesh.entities.Setting;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.EditTextWithDel;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * 地理位置
 */
public class LocationActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "LocationActivity";

    private TextView cancel, save;
    private EditTextWithDel edit_location;

    private Setting setting;
    private ProgressBarDialog progressDialog;

    private ExecutorService pool = Executors.newSingleThreadExecutor();
    private HTTPManagementAPI httpAPI = null;
    private DeviceStore deviceStore = null;
    private TextView location;
    private String[] locationArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        httpAPI = HTTPManagementAPI.getInstance();
        deviceStore = DeviceStore.getInstance(this);
        setting = deviceStore.getSetting();
        progressDialog = new ProgressBarDialog(this).createDialog(this);
        progressDialog.setMessage(getString(R.string.positioning));
        initFindViewById();
    }

    private void initFindViewById() {
        cancel = (TextView) findViewById(R.id.cancel);
        save = (TextView) findViewById(R.id.save);
        location = (TextView) findViewById(R.id.location);
        edit_location = (EditTextWithDel) findViewById(R.id.edit_location);
        cancel.setOnClickListener(this);
        save.setOnClickListener(this);
        handler.postDelayed(timeoutRunnable, 10 * 1500);
    }

    Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Message message = handler.obtainMessage();
            message.what = 0x001;
            handler.sendMessage(message);
        }
    };

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HTTPManagementAPI.PHONE_LOCATE_SUCCESS:
                    handler.removeCallbacks(timeoutRunnable);
                    progressDialog.dismiss();
                    locationArr= String.valueOf(msg.obj).split(",");
                    edit_location.setText(locationArr[1]);
                    /*"(" + setting.getCountryNO() + ")" + */
                    break;
                case HTTPManagementAPI.PHONE_LOCATE_FAILED:
                    handler.removeCallbacks(timeoutRunnable);
                    progressDialog.dismiss();
                    break;
                case 0x001:
                    handler.removeCallbacks(timeoutRunnable);
                    progressDialog.dismiss();
                    tips(getString(R.string.tips), getString(R.string.timeout)).setPositiveButton(R.string.yes, null).create().show();
                    break;
            }
        }
    };

    Runnable locateRunnable = new Runnable() {
        @Override
        public void run() {
            httpAPI.startLocate(LocationActivity.this, handler);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(timeoutRunnable);
    }

    @Override
    protected void onStart() {
        super.onStart();
        progressDialog.show();
        pool.execute(locateRunnable);
        edit_location.setText(setting.getLocate());
        location.setText(setting.getLocate());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                finish();
                break;
            case R.id.save:
                final String locate = edit_location.getText().toString().trim();
                if (locate.length() <= 0) {
                    tips(getString(R.string.tips), getString(R.string.not_empty)).setPositiveButton(R.string.yes, null).create().show();
                } else if (locate.length() < 2) {
                    tips(getString(R.string.tips), getString(R.string.enter_location_message)).setPositiveButton(R.string.yes, null).create().show();
                } else {
                    if(locationArr!=null){
                        setting.setCountryNO(locationArr[0]);
                        setting.setLocate(locationArr[1]);
                        setting.setCity(locationArr[2]);
                    }
                    setting.setLocate(locate);
                    deviceStore.updateSettings(setting);
                    finish();
                }
                break;
        }
    }

    private CustomDialog.Builder ibuilder;

    private CustomDialog.Builder tips(String title, String msg) {
        ibuilder = new CustomDialog.Builder(LocationActivity.this);
        ibuilder.setTitle(title);
        ibuilder.setMessage(msg);
        return ibuilder;
    }
}
