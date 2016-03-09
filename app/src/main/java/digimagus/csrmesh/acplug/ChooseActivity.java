package digimagus.csrmesh.acplug;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.digimagus.aclibrary.MessageService;
import com.digimagus.aclibrary.WiFiManagementAPI;

import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * 选择提示 连接WiFi
 */
public class ChooseActivity extends Activity implements View.OnClickListener {

    public static final String TAG = "ChooseActivity";

    private TextView next, cancel, pls_choose_wifi;
    private boolean addevice;
    private boolean first;
    private ProgressBarDialog mProgressBarDialog;

    private WiFiManagementAPI mWiFiManagementAPI;

    public MSGBroadcastReceiver receiver = new MSGBroadcastReceiver();

    class MSGBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {//监听网络状态变化
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo == null) {
                    Log.e(TAG, "当前的网络类型：没有网络");
                    MessageService.CURRENT_NETWORK_TYPE = MessageService.CONN_NETWORK_TYPE_NONE;
                } else if (netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    Log.e(TAG, "当前的网络类型：手机网络");
                    MessageService.CURRENT_NETWORK_TYPE = MessageService.CONN_NETWORK_TYPE_MOBILE;
                } else if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    Log.e(TAG, "当前的网络类型：WIFI网络");
                    MessageService.CURRENT_NETWORK_TYPE = MessageService.CONN_NETWORK_TYPE_WIFI;
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);

        mWiFiManagementAPI = WiFiManagementAPI.getInstance(ChooseActivity.this);
        mProgressBarDialog = new ProgressBarDialog(this).createDialog(this);


        first = getIntent().getBooleanExtra("first_time", false);
        next = (TextView) findViewById(R.id.next);
        cancel = (TextView) findViewById(R.id.cancel);
        pls_choose_wifi = (TextView) findViewById(R.id.pls_choose_wifi);
        next.setOnClickListener(this);
        cancel.setOnClickListener(this);


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory("receiver");
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, intentFilter);
    }

    private String wifiName1;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                if (MessageService.CURRENT_NETWORK_TYPE != MessageService.CONN_NETWORK_TYPE_WIFI) {
                    wifiName1 = mWiFiManagementAPI.getWiFiInfo().getSSID();
                    Intent setting = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(setting);
                    addevice = true;
                } else if (!wifiserver) {
                    tips(getString(R.string.check_network_conn)).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Intent setting = new Intent(Settings.ACTION_WIFI_SETTINGS);
                            startActivity(setting);
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (first) {
                                Intent intent = new Intent(ChooseActivity.this, MainActivity.class);
                                startActivity(intent);
                                ChooseActivity.this.finish();
                            } else {
                                ChooseActivity.this.finish();
                            }
                        }
                    }).create().show();
                }else{
                    wifiName1 = mWiFiManagementAPI.getWiFiInfo().getSSID();
                    Intent setting = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(setting);
                    addevice = true;
                }
                break;
            case R.id.cancel:
                if (first) {
                    Intent intent = new Intent(ChooseActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    finish();
                }
                break;
        }
    }

    public boolean wifiserver = false;

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart   " + (MessageService.CURRENT_NETWORK_TYPE == MessageService.CONN_NETWORK_TYPE_WIFI));
        if (MessageService.CURRENT_NETWORK_TYPE != MessageService.CONN_NETWORK_TYPE_WIFI) {
            tips(getString(R.string.no_wifi_pls_conn)).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    Intent setting = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(setting);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if (first) {
                        Intent intent = new Intent(ChooseActivity.this, MainActivity.class);
                        startActivity(intent);
                        ChooseActivity.this.finish();
                    } else {
                        ChooseActivity.this.finish();
                    }
                }
            }).create().show();
            pls_choose_wifi.setVisibility(View.GONE);
        }

        if (MessageService.CURRENT_NETWORK_TYPE == MessageService.CONN_NETWORK_TYPE_WIFI && !wifiserver) {
            mProgressBarDialog.show();
            handler.post(pingRunnable);
        } else if (addevice && !wifiName1.equals(mWiFiManagementAPI.getWiFiInfo().getSSID())) {
            Intent intent = new Intent(ChooseActivity.this, ChooseWiFiActivity.class);
            intent.putExtra("first_time", first);
            startActivity(intent);
            addevice = false;
            finish();
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mProgressBarDialog.dismiss();
            switch (msg.what) {
                case WiFiManagementAPI.PING_IP_SUCCESS:
                    wifiserver = true;
                    pls_choose_wifi.setVisibility(View.VISIBLE);
                    break;
                case WiFiManagementAPI.PING_IP_COMPLETE:
                    pingnum = 0;
                    Toast.makeText(ChooseActivity.this, getString(R.string.check_network_conn), Toast.LENGTH_SHORT).show();
                    break;
                case WiFiManagementAPI.PING_IP_FAILURE:
                    handler.postDelayed(pingRunnable, 2 * 1000);
                    break;
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(pingRunnable);
    }

    private int pingnum = 0;
    Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            Message message = handler.obtainMessage();
            if (mWiFiManagementAPI.ping("github.com")) {
                pingnum = 0;
                message.what = WiFiManagementAPI.PING_IP_SUCCESS;
            } else {
                pingnum++;
                if (pingnum >= 3) {
                    message.what = WiFiManagementAPI.PING_IP_COMPLETE;
                } else {
                    message.what = WiFiManagementAPI.PING_IP_FAILURE;
                }
            }
            handler.sendMessage(message);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private CustomDialog.Builder ibuilder;

    private CustomDialog.Builder tips(String msg) {
        ibuilder = new CustomDialog.Builder(ChooseActivity.this);
        ibuilder.setTitle(R.string.tips);
        ibuilder.setMessage(msg);
        return ibuilder;
    }
}
