package digimagus.csrmesh.acplug;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.digimagus.aclibrary.MessageService;

import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;
import digimagus.csrmesh.view.SwitchView;

/**
 *
 */
public class PrivacyActivity extends BaseActivity {
    private ProgressBarDialog progressDialog;
    private CustomDialog.Builder ibuilder;

    @Override
    protected void handler(Message msg) {
        switch (msg.what) {
            case SET_PRIVACY:
                progressDialog.dismiss();
                handler.removeCallbacks(delayRunnable);
                setPrivacy((Boolean) msg.obj);
                break;
        }
    }

    public void setPrivacy(boolean cy) {
        if (privacy != null && ((cy && privacy.getState() == 1) || (!cy && privacy.getState() == 4))) {
            privacy.setState(cy);
        }
        progressDialog.dismiss();
    }

    private SwitchView privacy;

    @Override
    void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_privacy);
        progressDialog = new ProgressBarDialog(this).createDialog(this);
        initFindViewById();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (MessageService.CURRENT_NETWORK_TYPE == MessageService.CONN_NETWORK_TYPE_NONE) {
            Toast.makeText(this, getString(R.string.No_network_connection), Toast.LENGTH_SHORT).show();
        } else {
            getPrivacy();
            progressDialog.show();
            handler.postDelayed(delayRunnable, 10 * 1000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(delayRunnable);
    }

    Runnable delayRunnable = new Runnable() {
        @Override
        public void run() {
            progressDialog.dismiss();
            handler.removeCallbacks(delayRunnable);
        }
    };

    private void initFindViewById() {
        privacy = (SwitchView) findViewById(R.id.privacy);
        privacy.setOnStateChangedListener(changedListener);
    }

    SwitchView.OnStateChangedListener changedListener = new SwitchView.OnStateChangedListener() {
        @Override
        public void toggleToOn(SwitchView view) {
            setPrivacy(view, true);
        }

        @Override
        public void toggleToOff(SwitchView view) {
            setPrivacy(view, false);
        }
    };


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x01:
                    progressDialog.setMessage(getString(R.string.set_privacy)).show();
                    break;
                case 0x02:
                    Tips(getString(R.string.network_tips), getString(R.string.not_access_internet)).setPositiveButton(R.string.yes, null).create().show();
                    break;
            }
        }
    };

    private void setPrivacy(SwitchView view, boolean status) {
        if (MessageService.CURRENT_NETWORK_TYPE == MessageService.CONN_NETWORK_TYPE_NONE) {
            Toast.makeText(this, getString(R.string.No_network_connection), Toast.LENGTH_SHORT).show();
        } else {
            handler.postDelayed(delayRunnable, 10 * 1000);
            Message message = handler.obtainMessage();
            if (MessageService.CURRENT_NETWORK_TYPE != MessageService.CONN_NETWORK_TYPE_NONE) {
                Log.e(TAG, "设置设备隐私...  "+status);
                message.what = 0x01;
                super.setPrivacy(status);
                view.setState(status);
            } else {
                Log.e(TAG, "设置设备隐私...  "+!status);
                message.what = 0x02;
                view.setState(!status);
            }
            handler.sendMessage(message);
        }
    }

    private CustomDialog.Builder Tips(String title, String msg) {
        ibuilder = new CustomDialog.Builder(this);
        ibuilder.setTitle(title);
        ibuilder.setMessage(msg);
        return ibuilder;
    }
}
