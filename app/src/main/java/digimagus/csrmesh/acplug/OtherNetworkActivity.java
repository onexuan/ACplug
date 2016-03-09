package digimagus.csrmesh.acplug;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
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

import java.util.ArrayList;
import java.util.List;

import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.EditTextWithDel;
import digimagus.csrmesh.view.ProgressBarDialog;
import digimagus.csrmesh.view.SwitchView;

/**
 * 选择WiFi连接
 */
public class OtherNetworkActivity extends Activity implements View.OnClickListener {

    private final static String TAG = "OtherNetworkActivity";

    private EditTextWithDel network, password;
    private TextView done;
    private SwitchView display;
    private String pass;
    private String wifiName;
    private WiFiManagementAPI wiFiManagementAPI;
    private WifiConfiguration configuration = null;
    private DeviceInfo info;


    private SocketManagementAPI socketManagementAPI;
    private ProgressBarDialog barDialog;
    private DeviceStore store;
    private String SSID;
    private WiFiManagementAPI.WiFiEncryptionType wifiType;

    private List<ScanResult> wifiLists = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_othernetwork);
        socketManagementAPI = SocketManagementAPI.getInstance();
        barDialog = new ProgressBarDialog(this).createDialog(this).setMessage(getString(R.string.conn_device));
        wiFiManagementAPI = WiFiManagementAPI.getInstance(this);
        store = DeviceStore.getInstance(this);
        Intent intent = getIntent();
        info = store.getDeviceInfo(intent.getStringExtra("serial"));
        if (info == null) {
            info = new DeviceInfo();
            info.setSerial(intent.getStringExtra("serial"));
        }
        initFindViewById();
    }

    private void initFindViewById() {
        network = (EditTextWithDel) findViewById(R.id.network);
        password = (EditTextWithDel) findViewById(R.id.password);
        done = (TextView) findViewById(R.id.done);
        display = (SwitchView) findViewById(R.id.display);
        display.setState(true);
        password.displayPassword(true);
        display.setOnStateChangedListener(stateChangedListener);
        done.setOnClickListener(this);

        wifiName = wiFiManagementAPI.getWiFiInfo().getSSID();
        wifiName = wifiName.substring(1, wifiName.length() - 1);
    }

    SwitchView.OnStateChangedListener stateChangedListener = new SwitchView.OnStateChangedListener() {
        @Override
        public void toggleToOn(SwitchView view) {
            view.setState(true);
            password.displayPassword(true);
        }

        @Override
        public void toggleToOff(SwitchView view) {
            view.setState(false);
            password.displayPassword(false);
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.done:
                SSID = network.getText().toString().trim();
                if ("".equals(SSID)) {
                    Tips(getString(R.string.enter_network_name)).setPositiveButton(R.string.yes, null).create().show();
                    return;
                }
                StringBuffer sb = new StringBuffer();
                sb.append("{\"devices\":\"*\",\"payload\":{\"wificonfig\":{\"mode\":1,\"ssid\":\"");
                sb.append(SSID);
                sb.append("\",\"password\":\"");
                sb.append(password.getText().toString().trim());
                sb.append("\"}}");
                sb.append("}");

                for (ScanResult result : wifiLists) {
                    Log.e(TAG, "比较  ：" + (SSID.equalsIgnoreCase(result.SSID)));
                    if (SSID.equalsIgnoreCase(result.SSID)) {
                        wifiType = wiFiManagementAPI.verifyWiFiType(result.capabilities);
                        SSID = result.SSID;
                        Log.e(TAG, " SSID: " + SSID);
                        break;
                    }
                }

                if ("".equals(password.getText().toString().trim())) {
                    if (wifiType == null) {
                        wifiType = WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_ESS;
                    }
                    configuration = wiFiManagementAPI.createWifiInfo(SSID, null, wifiType);
                } else {
                    if (wifiType == null) {
                        wifiType = WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_WPA;
                    }
                    configuration = wiFiManagementAPI.createWifiInfo(SSID, pass, wifiType);
                }
                barDialog.show();
                pass = sb.toString();
                handler.post(udpMsg);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        wifiLists = wiFiManagementAPI.getWifiList();
        for (ScanResult result : wifiLists) {
            Log.e(TAG, " " + result.SSID + "  " + result.capabilities);
        }
    }

    /**
     * 1、查找设备 发送UDP
     * 2、连接设备
     * 3、发送心跳
     * 4、接收消息
     * 5、切换WiF
     * 6、退出程序
     */
    private int num = 0;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MessageService.CONN_NETWORK_TYPE_NONE: {//无网络连接
                    Log.e(TAG, "   无网络连接  ");
                    num = 0;
                    break;
                }
                case MessageService.CONN_NETWORK_TYPE_WIFI: {//WiFi网络连接
                    Log.e(TAG, "   WiFi网络连接 ");
                    num++;
                    break;
                }
                case MessageService.CONN_NETWORK_TYPE_MOBILE: {//移动网络连接
                    Log.e(TAG, "   移动网络连接 ");
                    num++;
                    break;
                }
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
                case MessageService.SOCKET_CONN_DISCONNECT: {
                    Log.e(TAG, "SOCKET 断开 ： ");
                    //barDialog.dismiss();
                    break;
                }
                case MessageService.DEVICE_BACK_HEARTBEAT: {
                    Log.e(TAG, "心跳消息： ");
                    break;
                }
                case MessageService.DEVICE_BACK_PARAMETER: {
                    Log.e(TAG, "读取参数： ");
                    break;
                }
                case MessageService.DEVICE_BACK_UNKONW: {

                    break;
                }
                case MessageService.DEVICE_BACK_SETPARAMETER: {
                    socketManagementAPI.stopThread();
                    barDialog.setMessage(getString(R.string.device_configuration));
                    wiFiManagementAPI.addWiFiNetwork(configuration);
                    handler.postDelayed(rebootWiFi, MessageService.REBOOT_WIFI / 2 * 3);
                    break;
                }
            }
        }
    };

    /**
     * 重启WiFi 延时
     */
    Runnable rebootWiFi = new Runnable() {
        @Override
        public void run() {
            barDialog.dismiss();
            if (wiFiManagementAPI.getWiFiInfo().getSSID().equals("\"" + SSID + "\"") || num >= 1) {
                Intent region = new Intent(OtherNetworkActivity.this, RegionActivity.class);
                region.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));
                region.putExtra("mac", info.getMac());
                region.putExtra("serial", info.getSerial());
                region.putExtra("name", wifiName);
                Log.e(TAG, "序列号：    " + info.getSerial());
                startActivity(region);
                Log.e(TAG, "密码设置成功....." + info.getSerial());
                finish();
            } else {
                Log.e(TAG, "密码设置失败.....");
                Tips(getString(R.string.conn_device_fail)).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent choose = new Intent(OtherNetworkActivity.this, ChooseActivity.class);
                        choose.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));
                        startActivity(choose);
                        finish();
                    }
                }).create().show();
            }
        }
    };


    /**
     * 发送密码
     */
    Runnable send = new Runnable() {
        @Override
        public void run() {
            socketManagementAPI.sendMessage(pass, (byte) MessageService.SET_PARAMETER_PACKAGE);
        }
    };


    private CustomDialog.Builder ibuilder;

    private CustomDialog.Builder Tips(String msg) {
        ibuilder = new CustomDialog.Builder(OtherNetworkActivity.this);
        ibuilder.setTitle(R.string.tips);
        ibuilder.setMessage(msg);
        return ibuilder;
    }
}
