package digimagus.csrmesh.acplug;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.digimagus.aclibrary.MessageService;
import com.digimagus.aclibrary.SocketManagementAPI;
import com.digimagus.aclibrary.WiFiManagementAPI;

import org.json.JSONException;
import org.json.JSONObject;

import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.EditTextWithDel;
import digimagus.csrmesh.view.ProgressBarDialog;
import digimagus.csrmesh.view.SwitchView;

public class ConnWiFIActivity extends BaseActivity implements View.OnClickListener {
    private final static String TAG = "ConnWiFIActivity";
    private SwitchView display;
    private EditTextWithDel password;
    private TextView done;

    private ProgressBarDialog barDialog;
    private WiFiManagementAPI wiFiManagementAPI;
    private SocketManagementAPI socketManagementAPI;
    private WiFiManagementAPI.WiFiEncryptionType wifiType;
    private String SSID = null;
    private String pass;
    private WifiConfiguration configuration = null;
    private DeviceInfo info;
    private DeviceStore store;

    @Override
    protected void handler(Message msg) {
        switch (msg.what){
            case PHONE_FIND_DEVICE: {
                try {
                    JSONObject obj=new JSONObject(String.valueOf(msg.obj));
                    if(obj.has("SERIAL")){
                        if(info.getSerial().equals(obj.getString("SERIAL"))){
                            Log.e(TAG, "设备配置成功..........");
                            config=true;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_connwifi);
        socketManagementAPI = SocketManagementAPI.getInstance();
        wiFiManagementAPI = WiFiManagementAPI.getInstance(this);
        store = DeviceStore.getInstance(this);
        Intent intent = getIntent();
        info = store.getDeviceInfo(intent.getStringExtra("serial"));
        SSID = intent.getStringExtra("SSID");
        if (info == null) {
            info = new DeviceInfo();
            info.setSerial(intent.getStringExtra("serial"));
        }
        info.setDevsn(intent.getStringExtra("devsn"));
        info.setDevtype(intent.getStringExtra("type"));
        Log.e(TAG, "序列号： " + info.getSerial());
        initFindViewById();
        barDialog = new ProgressBarDialog(this).createDialog(this).setMessage(getString(R.string.conn_device));
        Log.e(TAG, "   " + intent.getStringExtra("TYPE"));
        wifiType = wiFiManagementAPI.verifyWiFiType(intent.getStringExtra("TYPE"));
    }

    private void initFindViewById() {
        display = (SwitchView) findViewById(R.id.display);
        password = (EditTextWithDel) findViewById(R.id.password);

        done = (TextView) findViewById(R.id.done);
        done.setOnClickListener(this);
        display.setOnStateChangedListener(switchListener);
        wifiName = wiFiManagementAPI.getWiFiInfo().getSSID();
        wifiName = wifiName.substring(1, wifiName.length() - 1);

        display.setState(true);
        password.displayPassword(true);
    }

    /**
     * 1、查找设备 发送UDP
     * 2、连接设备
     * 3、发送心跳
     * 4、接收消息
     * 5、切换WiF
     * 6、退出程序
     */

    private boolean config=false;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MessageService.SOCKET_CONN_SUCCESS: {
                    Log.e(TAG, "Socket 连接成功 ： ");
                    barDialog.setMessage(getString(R.string.send_message));
                    socketManagementAPI.startThread(handler);
                    handler.post(send);
                    break;
                }
                case MessageService.SOCKET_CONN_FAILURE: {
                    Log.e(TAG, "SOCKET 连接失败 ： ");
                    barDialog.dismiss();
                    break;
                }
                case MessageService.SOCKET_MESSAGE_SUCCESS: {
                    barDialog.setMessage(getString(R.string.message_send_success));
                    Log.e(TAG, "SOCKET 消息发送成功 ： ");
                    break;
                }
                case MessageService.DEVICE_BACK_SETPARAMETER: {
                    socketManagementAPI.stopThread();
                    barDialog.setMessage(getString(R.string.device_configuration));
                    wiFiManagementAPI.addWiFiNetwork(configuration);
                    handler.postDelayed(rebootWiFi, MessageService.REBOOT_WIFI);
                    break;
                }
            }
        }
    };

    private String wifiName;

    /**
     * 重启WiFi 延时
     */
    Runnable rebootWiFi = new Runnable() {
        @Override
        public void run() {
            barDialog.dismiss();
            if (wiFiManagementAPI.getWiFiInfo().getSSID().equals("\"" + SSID + "\"")&&config) {
                Intent region = new Intent(ConnWiFIActivity.this, RegionActivity.class);
                region.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));
                region.putExtra("mac", info.getMac());
                region.putExtra("serial", info.getSerial());
                region.putExtra("name", wifiName);
                Log.e(TAG, "序列号：    " + info.getSerial());
                startActivity(region);
                finish();
                Log.e(TAG, "密码设置成功....." + info.getSerial());
            } else {
                Log.e(TAG, "密码设置失败.....");
                tips(getString(R.string.conn_device_fail)).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent choose = new Intent(ConnWiFIActivity.this, ChooseActivity.class);
                        choose.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));
                        startActivity(choose);
                        finish();
                    }
                }).create().show();
            }
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(ConnWiFIActivity.this, ChooseWiFiActivity.class);
        intent.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));
        startActivity(intent);
        finish();
    }

    /**
     * 发送密码
     */
    Runnable send = new Runnable() {
        @Override
        public void run() {
            socketManagementAPI.sendMessage(pass, (byte) MessageService.SET_PARAMETER_PACKAGE);
        }
    };

    Runnable heartBeat = new Runnable() {
        @Override
        public void run() {
            socketManagementAPI.sendMessage(MessageService.SEND_SOCKET_HEART);

            socketManagementAPI.sendMessage(MessageService.READ_SOCKET_HEART);

            handler.postDelayed(heartBeat, 5 * 1000);
        }
    };

    //发送UDP 消息
    Runnable udpMsg = new Runnable() {
        @Override
        public void run() {
            barDialog.setMessage(getString(R.string.conn_device));
            socketManagementAPI.conn("192.168.4.1", 8803, handler);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (wifiType == WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_ESS || wifiType == WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_UNKNOWN) {//无密码
            password.setHint(getString(R.string.no_need_pass));
            password.setEnabled(false);
        } else if (wifiType == WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_WEP || wifiType == WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_WPA) {//需要密码
            password.setHint(getString(R.string.enter_password));
            password.setEnabled(true);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.done:
                config=false;
                pass = password.getText().toString().trim();
                if (WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_ESS != wifiType) {
                    if (pass.length() < 8) {
                        tips(getString(R.string.pass_at_least_eight)).setPositiveButton(R.string.yes, null).create().show();
                        return;
                    }
                }
                configuration = wiFiManagementAPI.createWifiInfo(SSID, pass, wifiType);
                StringBuffer sb = new StringBuffer();
                sb.append("{\"devices\":\"*\",\"payload\":{\"wificonfig\":{\"mode\":1,\"ssid\":\"");
                sb.append(SSID);
                sb.append("\",\"password\":\"");
                sb.append(pass);
                sb.append("\"}}");
                sb.append("}");
                pass = sb.toString();
                barDialog.show();
                handler.post(udpMsg);
                break;
        }
    }

    private void updateSwitch(boolean state) {
        display.toggleSwitch(state);
        password.displayPassword(state);
    }

    private SwitchView.OnStateChangedListener switchListener = new SwitchView.OnStateChangedListener() {
        @Override
        public void toggleToOn(SwitchView view) {
            updateSwitch(true);
        }

        @Override
        public void toggleToOff(SwitchView view) {
            updateSwitch(false);
        }
    };


    private CustomDialog.Builder ibuilder;
    private CustomDialog.Builder tips(String msg) {
        ibuilder = new CustomDialog.Builder(ConnWiFIActivity.this);
        ibuilder.setTitle(R.string.tips);
        ibuilder.setMessage(msg);
        return ibuilder;
    }
}