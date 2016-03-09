package digimagus.csrmesh.acplug;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.digimagus.aclibrary.MessageService;
import com.digimagus.aclibrary.WiFiManagementAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * 选择WiFi
 */
public class ChooseWiFiActivity extends BaseActivity {
    private final static String TAG = "ChooseWiFiActivity";

    private ListView wifi_list;
    private List<ScanResult> wifiLists = new ArrayList<>();
    private ChooseWiFiConnAdapter wiFiConnAdapter = null;
    private ProgressBarDialog barDialog;

    private DeviceInfo deviceInfo;

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_choosewifi);
        deviceInfo = new DeviceInfo();
        initFindViewById();
        barDialog = new ProgressBarDialog(this).createDialog(this);
    }

    private void initFindViewById() {
        View view=LayoutInflater.from(this).inflate(R.layout.item_footview,null);
        wifi_list = (ListView) findViewById(R.id.wifi_list);
        wiFiConnAdapter = new ChooseWiFiConnAdapter(this);
        wifi_list.addFooterView(view);
        wifi_list.setAdapter(wiFiConnAdapter);
        wifi_list.setOnItemClickListener(onItemListener);
    }

    /**
     * 发送设备发现广播
     */
    private void sendFindDeviceUDP() {
        barDialog.setMessage(getString(R.string.scanning_device)).show();
        wiFiManagementAPI.startScanWifi();
        wifiLists = wiFiManagementAPI.getWifiList();
        wiFiConnAdapter.notifyDataSetChanged();
        starttime = System.currentTimeMillis();
        handler.post(findDevice);
    }

    Runnable findDevice = new Runnable() {
        @Override
        public void run() {
            endtime = System.currentTimeMillis();
            if (endtime - starttime > 10 * 1000) {
                Message message = handler.obtainMessage();
                message.what = MessageService.FIND_DEVICE_FAILURE;
                handler.sendMessage(message);
            } else {
                handler.postDelayed(this, MessageService.FIND_DEVICE_DELAYED);
            }
        }
    };
    private boolean sendudp=true;
    @Override
    protected void onPause() {
        super.onPause();
        sendudp=false;
    }

    private void switchWiFi(String parameter, final boolean state) {
        tips(parameter).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (state) {
                    sendudp=true;
                    sendFindDeviceUDP();
                } else {
                    Intent setting = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(setting);
                }
            }
        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(ChooseWiFiActivity.this, ChooseActivity.class);
                intent.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));
                startActivity(intent);
                finish();
            }
        }).create().show();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(!sendudp){
                return;
            }
            switch (msg.what) {
                case MessageService.FIND_DEVICE_FAILURE: {
                    barDialog.dismiss();
                    String wifiname = wiFiManagementAPI.getWiFiInfo().getSSID();
                    if (wifiname.equals("<unknown ssid>")) {
                        Log.e(TAG, "请开启手机WiFi");
                        switchWiFi(getString(R.string.turn_on_wifi), false);
                    } else if (wifiname.equals("0x")) {
                        Log.e(TAG, "手机未连接到WiFi");
                        switchWiFi(getString(R.string.conn_device_wifi), false);
                    } else {
                        Log.e(TAG, "手机连接到WiFi :" + wifiname);
                        switchWiFi(getString(R.string.not_find_device, wifiname), true);
                    }
                    break;
                }
                case MessageService.UDP_READ_SUCCESS: {
                    try {
                        JSONObject obj = new JSONObject(String.valueOf(msg.obj));
                        String ip = obj.getString("IP");
                        String serial = obj.getString("SERIAL");
                        String type = obj.getString("type");
                        String firm = obj.getString("firm");
                        Log.e(TAG, "UDP : " + msg.obj);
                        if (!"100301".equals(type) || !"150002".equals(firm)) {
                            return;
                        }
                        //Log.e(TAG, "UDP " + msg.obj);
                        if (ip.equals("192.168.4.1")) {
                            handler.removeCallbacks(findDevice);
                            barDialog.dismiss();
                            deviceInfo.setSerial(serial);
                            deviceInfo.setDevsn(type);
                            deviceInfo.setDevtype(type);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    };

    private AdapterView.OnItemClickListener onItemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            Log.e(TAG,"position  "+position+"    "+wifiLists.size());
            if (wifiLists.size()==position){
                Intent connintent = new Intent(ChooseWiFiActivity.this, OtherNetworkActivity.class);
                connintent.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));

                connintent.putExtra("serial", deviceInfo.getSerial());
                connintent.putExtra("devsn", deviceInfo.getDevsn());
                connintent.putExtra("type", deviceInfo.getDevtype());
                startActivity(connintent);
            }else{
                ScanResult scanResult = wifiLists.get(position);
                Intent connintent = new Intent(ChooseWiFiActivity.this, ConnWiFIActivity.class);
                connintent.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));
                connintent.putExtra("SSID", scanResult.SSID);
                connintent.putExtra("TYPE", scanResult.capabilities);
                connintent.putExtra("serial", deviceInfo.getSerial());
                connintent.putExtra("devsn", deviceInfo.getDevsn());
                connintent.putExtra("type", deviceInfo.getDevtype());
                startActivity(connintent);
            }
            ChooseWiFiActivity.this.finish();
        }
    };
    private long starttime = 0;
    private long endtime = 0;

    Runnable scanWiFiRunnable = new Runnable() {
        @Override
        public void run() {
            wiFiManagementAPI.startScanWifi();
            wifiLists = wiFiManagementAPI.getWifiList();
            wiFiConnAdapter.notifyDataSetChanged();
            endtime = System.currentTimeMillis();
            if (endtime - starttime >= 10 * 1000) {
                barDialog.dismiss();
            } else if (endtime - starttime >= 9 * 1000) {
                barDialog.setMessage(getString(R.string.scan_complete));
                handler.postDelayed(scanWiFiRunnable, 1000);
            } else {
                handler.postDelayed(scanWiFiRunnable, 1000);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        sendudp=true;
        String wifiname = wiFiManagementAPI.getWiFiInfo().getSSID();
        Log.e(TAG, "Name : " + wifiname);
        if (wifiname.equals("<unknown ssid>")||wifiname.equals("0x")) {
            Log.e(TAG, "请开启手机WiFi");
            //switchWiFi(getString(R.string.turn_on_wifi), false);
            Intent intent=new Intent(ChooseWiFiActivity.this,ChooseActivity.class);
            intent.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));
            ChooseWiFiActivity.this.startActivity(intent);
            ChooseWiFiActivity.this.finish();
        }/* else if (wifiname.equals("0x")) {
            Log.e(TAG, "手机未连接到WiFi");
            //switchWiFi(getString(R.string.conn_device_wifi), false);
            Intent intent=new Intent(ChooseWiFiActivity.this,ChooseActivity.class);
            intent.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));
            ChooseWiFiActivity.this.startActivity(intent);
            ChooseWiFiActivity.this.finish();
        }*/else {
            Log.e(TAG, "手机连接到WiFi :" + wifiname);
            sendudp=true;
            sendFindDeviceUDP();
            //switchWiFi(getString(R.string.confirm_config, wifiname), true);
        }
    }

    private CustomDialog.Builder ibuilder;

    private CustomDialog.Builder tips(String msg) {
        ibuilder = new CustomDialog.Builder(ChooseWiFiActivity.this);
        ibuilder.setTitle(R.string.tips);
        ibuilder.setMessage(msg);
        return ibuilder;
    }

    class ChooseWiFiConnAdapter extends BaseAdapter {
        private Context context;

        public ChooseWiFiConnAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return wifiLists.size();
        }

        @Override
        public Object getItem(int position) {
            return wifiLists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ScanResult result = wifiLists.get(position);
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.item_choose_wifi, null);
                holder.wifiName = (TextView) convertView.findViewById(R.id.wifi_name);
                holder.encryption = (ImageView) convertView.findViewById(R.id.encryption);
                holder.signal = (ImageView) convertView.findViewById(R.id.wifi_signal);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.wifiName.setText(result.SSID);
            if (result.level > -45) {
                holder.signal.setImageResource(R.drawable.ic_wifi_full);
            } else if (result.level > -65) {
                holder.signal.setImageResource(R.drawable.ic_wifi_2);
            } else if (result.level > -75) {
                holder.signal.setImageResource(R.drawable.ic_wifi_1);
            } else {
                holder.signal.setImageResource(R.drawable.ic_wifi_0);
            }
            if (wiFiManagementAPI.verifyWiFiType(result.capabilities) == WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_UNKNOWN ||
                    wiFiManagementAPI.verifyWiFiType(result.capabilities) == WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_ESS) {
                holder.encryption.setVisibility(View.INVISIBLE);
            } else {
                holder.encryption.setVisibility(View.VISIBLE);
            }
            return convertView;
        }

        class ViewHolder {
            private TextView wifiName;
            private ImageView encryption;
            private ImageView signal;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(ChooseWiFiActivity.this, ChooseActivity.class);
        intent.putExtra("first_time", getIntent().getBooleanExtra("first_time", false));
        startActivity(intent);
        finish();
    }

    @Override
    protected void handler(Message msg) {
        switch (msg.what){
            case PHONE_FIND_DEVICE:
                Log.e(TAG, "udp: " + msg.obj);
                Message message=handler.obtainMessage();
                message.what=MessageService.UDP_READ_SUCCESS;
                message.obj=msg.obj;
                handler.sendMessage(message);
                break;
        }
    }
}
