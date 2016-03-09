package digimagus.csrmesh.acplug;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.digimagus.aclibrary.HTTPManagementAPI;
import com.digimagus.aclibrary.MessageService;
import com.digimagus.aclibrary.WiFiManagementAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import digimagus.csrmesh.acplug.fragment.DeviceMainFragment;
import digimagus.csrmesh.acplug.fragment.GroupMainFragment;
import digimagus.csrmesh.acplug.fragment.MoreFragment;
import digimagus.csrmesh.acplug.listener.DeviceMainFragmentListener;
import digimagus.csrmesh.acplug.listener.DeviceStatusListener;
import digimagus.csrmesh.acplug.listener.FragmentController;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.GroupDevice;
import digimagus.csrmesh.entities.HideDeviceInfo;
import digimagus.csrmesh.entities.PhoneInfo;
import digimagus.csrmesh.entities.Setting;
import digimagus.csrmesh.view.CustomDialog;

public class MainActivity extends BaseActivity implements View.OnClickListener, FragmentController, BaseActivity.UpdateDeviceStateListener {

    private ImageView individual, group, more;
    private FragmentManager fragmentManager;
    private FragmentTransaction mTransaction;
    private Fragment currentfragment;


    public ProgressDialog progressBar;
    private DeviceStatusListener statusListener;
    private DeviceMainFragmentListener deviceListListener;
    /**
     * 远程模式 本地模式的连接状态
     * true  表示可用
     * false 表示不可用
     */
    private boolean remote_mode = false;

    private ExecutorService pool = Executors.newFixedThreadPool(3);
    private final static String TAG = "MainActivity";
    private int pingnum = 0;

    @Override
    protected void handler(Message msg) {
        try {
            if (msg.what == NOTYFY_UI_CHANGE) {
                String serial = String.valueOf(msg.obj);
                DeviceInfo device = devices.get(serial);
                if (devices.get(serial) != null) {
                    deviceListListener.updateDeviceStatus(serial, device.online, device.state, device.power, device.remaining);
                    if (statusListener != null) {
                        statusListener.deviceStatus(serial, device.state == 1 ? true : false, true);
                    }
                }
            } else {
                JSONObject obj = new JSONObject(String.valueOf(msg.obj));
                switch (msg.what) {
                    case PHONE_FIND_DEVICE: {
                        String type = obj.getString("type");
                        String firm = obj.getString("firm");
                        if (!"100301".equals(type) || !"150002".equals(firm)) {
                            return;
                        }
                        String ip = obj.getString("IP");
                        String serial = obj.getString("SERIAL");
                        DeviceInfo info = devices.get(serial);
                        if (info == null && hideDevices.get(serial) == null) {
                            info = new DeviceInfo();
                            info.setDevsn(firm);
                            info.setDevtype(type);
                            info.setSerial(serial);
                            info.setName("Outlet-" + info.getSerial().substring(info.getSerial().length() - 4, info.getSerial().length()));
                            info.setMac(obj.getString("MAC"));
                            info.setIP(ip);
                            info.setPORT(obj.getInt("PROT"));
                            if (deviceListListener != null) {
                                deviceListListener.queryDeviceList(info);
                            }
                            mDeviceStore.addDeviceInfo(info);
                            devices.put(serial, info);
                            queryDeviceState(info, 1);
                        } else if (info != null && info.getIP() == null) {
                            info.setDevsn(firm);
                            info.setDevtype(type);
                            info.online = true;
                            if (info.getIP() == null || info.getPORT() == 0) {
                                info.setPORT(obj.getInt("PROT"));
                                info.setIP(ip);
                                if (deviceListListener != null) {
                                    deviceListListener.queryDeviceList(info);
                                }
                                queryDeviceState(info, 1);
                            }
                        }
                        break;
                    }
                    case PHONE_SET_PARAMEETER_RESULT: {
                        Log.e(TAG, "参数设置成功。。。:" + obj);
                        break;
                    }
                    case DEVICE_BACK_STATE1: {
                        String serial;
                        if (obj.has("topic")) {
                            obj = obj.getJSONObject("data");
                            serial = mUUIDSERIAL.get(obj.getString("fromUuid"));
                        } else {
                            serial = obj.getString("devices");
                        }
                        obj = obj.getJSONObject("payload");
                        if (obj.has("switch")) {
                            int status = obj.getInt("switch");
                            DeviceInfo receive = getDevice(serial);
                            if (receive == null) {
                                return;
                            }
                            if (obj.has("remaining")) {
                                receive.remaining = obj.getInt("remaining");
                            }
                            receive.setPower(obj.getDouble("Power") / 100);
                            if (receive.getSerial() != null) {
                                deviceListListener.updateDeviceStatus(receive.getSerial(), true, status, receive.power, receive.remaining);
                                if (statusListener != null) {
                                    statusListener.deviceStatus(receive.getSerial(), status == 1 ? true : false, true);
                                }
                            }
                            devices.get(serial).online = true;
                            devices.get(serial).state = status;
                        }
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateDeviceState(String serial) {
        DeviceInfo device = devices.get(serial);
        if (device != null) {
            deviceListListener.updateDeviceStatus(serial, device.online, device.state, device.power, device.remaining);
            if (statusListener != null) {
                statusListener.deviceStatus(serial, devices.get(serial).state == 1 ? true : false, device.online);
            }
        }
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        fragmentManager = getFragmentManager();
        initFindViewById();

        //调试图表数据
        /*DeviceInfo info=new DeviceInfo();
        info.setName("test");
        info.setUuid("ce7a60ba-0b1d-46d7-8eb3-59f2bb352cfa");
        info.setSerial("12345678");
        mDeviceStore.addDeviceInfo(info);*/

        //定时请求设备的状态
        if ("".equals(phoneInfo.getUuid())) {
            Log.e(TAG, " 用户需要注册。。。。。  ");
            Intent mIntent = new Intent(MessageService.USER_REGISTER_PHONE);
            sendBroadcast(mIntent);
        } else {
            Log.e(TAG, "连接MQTT  PHONE - UUID:  " + phoneInfo.getUuid() + "  TOKEN  " + phoneInfo.getToken() + "  " + devices.size());
            connMQTTServer();
        }
        //定时请求设备信息
        handler.post(mTimerRequestDeviceState);
    }

    //定时查询设备的状态
    Runnable mTimerRequestDeviceState = new Runnable() {
        @Override
        public void run() {
            for (DeviceInfo info : devices.values()) {
                if ((System.currentTimeMillis() - info.sendtime >= 30 * 1000 && info.online && info.sendtime != 0) || request) {
                    request = false;
                    queryDeviceState(devices.get(info.getSerial()), 1);
                    devices.get(info.getSerial()).sendtime = System.currentTimeMillis();
                }
            }
            handler.postDelayed(this, 15 * 1000);
        }
    };

    /**
     * 设置设备的状态
     * <p/>
     * 根据设备现在的状态取反
     *
     * @param info
     */
    @Override
    public void setDeviceState(DeviceInfo info, View v) {
        info = devices.get(info.getSerial());
        if (System.currentTimeMillis() - info.sendmsgtime >1000) {
            v.findViewById(R.id.send_msg).setVisibility(View.VISIBLE);
            ((ImageView) v.findViewById(R.id.device_state)).setImageResource(R.mipmap.icon_activated);
            Log.e(TAG, " 点击....................大于1s  "+(System.currentTimeMillis() - info.sendmsgtime)/1000);
            info.sendmsgtime = System.currentTimeMillis();

            super.setDeviceState(info);
        }else{
            Log.e(TAG, " 点击....................少于1s  "+(System.currentTimeMillis() - info.sendmsgtime)/1000);
        }
    }


    @Override
    public void getAllDeviceState() {
        for (DeviceInfo d : devices.values()) {
            if (d != null) {
                queryDeviceState(d, 1);
            }
        }
    }

    @Override
    public void queryAllDeviceState() {
        //查询Http服务器
        new Thread(queryRunnable).start();
        //发送请求设备的状态
        for (DeviceInfo info : devices.values()) {
            queryDeviceState(info, 1);
        }
    }

    private boolean request = false;

    @Override
    protected void onStart() {
        super.onStart();
        setUpdateDeviceState(this);
        request = true;
        for (DeviceInfo d : mDeviceStore.getAllDevices().values()) {
            if (devices.get(d.getSerial()) == null) {
                devices.put(d.getSerial(), d);
            } else {
                devices.get(d.getSerial()).setName(d.getName());
            }
        }
        if (deviceListListener != null) {
            deviceListListener.refresh();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            Message message = mainHandler.obtainMessage();
            if (wiFiManagementAPI.ping("github.com")) {
                pingnum = 0;
                message.what = WiFiManagementAPI.PING_IP_SUCCESS;
            } else {
                pingnum++;
                if (pingnum >= 3) {
                    message.what = MessageService.PING_NETWORK_FAILURE;
                } else {
                    message.what = WiFiManagementAPI.PING_IP_FAILURE;
                }
            }
            mainHandler.sendMessage(message);
        }
    };
    private final Handler mainHandler = new MainHandler(this);


    private class MainHandler extends Handler {
        private final WeakReference<MainActivity> activity;

        public MainHandler(MainActivity context) {
            activity = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity act = activity.get();
            switch (msg.what) {
                case WiFiManagementAPI.PING_IP_SUCCESS://IP Ping 成功
                    act.remote_mode = true;
                    sendMessage(obtainMessage(MessageService.START_CONNECTION));
                    Log.e(TAG, "PING 成功");
                    break;
                case WiFiManagementAPI.PING_IP_FAILURE://IP Ping 失败
                    act.remote_mode = false;
                    sendMessage(obtainMessage(MessageService.START_CONNECTION));
                    act.pool.execute(pingRunnable);
                    Log.e(TAG, "PING 失败");
                    break;
                case MessageService.PING_NETWORK_FAILURE:
                    Log.e(TAG, "超过三次Ping 失败。。。。。");
                    for (DeviceInfo d : devices.values()) {
                        d.online = false;
                        deviceListListener.updateDeviceStatus(d.getSerial(), false, 2, 0, 0);
                    }
                    if (MessageService.CURRENT_NETWORK_TYPE != MessageService.CONN_NETWORK_TYPE_WIFI) {
                        Toast.makeText(MainActivity.this, getString(R.string.check_network_conn), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MessageService.START_CONNECTION: {
                    if (MessageService.CURRENT_NETWORK_TYPE != MessageService.CONN_NETWORK_TYPE_NONE) {
                        progressBar.dismiss();
                        post(act.runnableGroup);
                    } else {
                        act.progressBar.dismiss();
                    }
                    break;
                }
                case MessageService.CONN_NETWORK_TYPE_NONE: {//无网络连接
                    MessageService.CURRENT_NETWORK_TYPE = MessageService.CONN_NETWORK_TYPE_NONE;
                    sendMessage(obtainMessage(MessageService.START_CONNECTION));
                    removeCallbacks(act.runnableGroup);
                    break;
                }
                case MessageService.CONN_NETWORK_TYPE_MOBILE: {//手机网络连接
                    Log.e(TAG, "手机网络连接...");
                    MessageService.CURRENT_NETWORK_TYPE = MessageService.CONN_NETWORK_TYPE_MOBILE;
                    act.pool.execute(pingRunnable);
                    removeCallbacks(act.runnableGroup);
                    break;
                }
                case MessageService.CONN_NETWORK_TYPE_WIFI: {//WiFi网络连接
                    Log.e(TAG, "WiFi网络连接...");
                    MessageService.CURRENT_NETWORK_TYPE = MessageService.CONN_NETWORK_TYPE_WIFI;
                    act.pool.execute(pingRunnable);
                    removeCallbacks(act.runnableGroup);
                    break;
                }
            }
        }
    }


    @Override
    public List<DeviceInfo> getDevices(Integer groupId) {
        List<DeviceInfo> devices = new ArrayList<>();
        for (DeviceInfo d : mGroupDevices.get(groupId).getDevices().values()) {
            d.online = this.devices.get(d.getSerial()).online;
            d.state = this.devices.get(d.getSerial()).state;
            devices.add(d);
        }
        return devices;
    }

    public DeviceInfo getDeviceDevs(String param) {
        DeviceInfo device = null;
        for (DeviceInfo info : devices.values()) {
            if (info.getUuid() != null && info.getUuid().equals(param)) {
                device = info;
                break;
            }
        }
        return device;
    }

    @Override
    public DeviceInfo getDevice(String serial) {
        return devices.get(serial);
    }

    @Override
    public void setStatusListener(DeviceStatusListener statusListener) {
        this.statusListener = statusListener;
    }


    private List<Fragment> fragments = new ArrayList<>();

    private void initFindViewById() {
        individual = (ImageView) findViewById(R.id.individual);
        group = (ImageView) findViewById(R.id.group);
        more = (ImageView) findViewById(R.id.more);
        individual.setOnClickListener(this);
        more.setOnClickListener(this);
        group.setOnClickListener(this);

        fragments.add(new DeviceMainFragment());
        fragments.add(new GroupMainFragment());
        fragments.add(new MoreFragment());

        updateFragment(fragments.get(0));
        if (progressBar == null) {
            progressBar = new ProgressDialog(this);
        }
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setMessage(getString(R.string.detecting_network));
        progressBar.setCancelable(false);
        progressBar.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.individual:
                updateFragment(fragments.get(0));
                individual.setImageResource(R.mipmap.icon_individual_blue);
                group.setImageResource(R.mipmap.icon_group_white);
                more.setImageResource(R.mipmap.icon_more_white);
                break;
            case R.id.group:
                updateFragment(fragments.get(1));
                individual.setImageResource(R.mipmap.icon_individual_white);
                group.setImageResource(R.mipmap.icon_group_blue);
                more.setImageResource(R.mipmap.icon_more_white);
                break;
            case R.id.more:
                updateFragment(fragments.get(2));
                individual.setImageResource(R.mipmap.icon_individual_white);
                group.setImageResource(R.mipmap.icon_group_white);
                more.setImageResource(R.mipmap.icon_more_blue);
                break;
        }
    }

    private void updateFragment(Fragment fragment) {
        mTransaction = fragmentManager.beginTransaction();
        //mTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);  //Fragment跳转动画
        //mTransaction.setCustomAnimations(android.R.anim.fade_in, R.anim.slide_out);
        if (fragment == currentfragment) {
            return;
        }
        if (currentfragment != null) {
            if (!fragment.isAdded()) {
                mTransaction.hide(currentfragment).add(R.id.frameLayout, fragment); // 隐藏当前的fragment，add下一个到Activity中
            } else {
                mTransaction.hide(currentfragment).show(fragment); // 隐藏当前的fragment，显示下一个
            }
        } else {
            mTransaction.add(R.id.frameLayout, fragment);
        }
        mTransaction.commit();
        currentfragment = fragment;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) { //按下的如果是BACK，同时没有重复
            Tips(getString(R.string.quit_tips), getString(R.string.quit)).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            }).setNegativeButton(R.string.no, null).create().show();
        }
        return false;
    }

    @Override
    public Map<String, DeviceInfo> getAllDevice() {
        for (DeviceInfo d : mDeviceStore.getAllDevices().values()) {
            if (devices.get(d.getSerial()) == null) {
                devices.put(d.getSerial(), d);
            } else {
                devices.get(d.getSerial()).setName(d.getName());
            }
        }
        return devices;
    }

    @Override
    public List<GroupDevice> getAllGroups() {
        List<GroupDevice> groupDevices = new ArrayList<>();
        for (GroupDevice group : mGroupDevices.values()) {
            groupDevices.add(group);
        }
        return groupDevices;
    }

    /**
     * 移除一个组
     *
     * @param groupId
     */
    @Override
    public void removeGroupById(int groupId) {
        mDeviceStore.removeGroupById(groupId);
    }

    @Override
    public void setDeviceListListener(DeviceMainFragmentListener deviceListListener) {
        this.deviceListListener = deviceListListener;
    }

    @Override
    public Setting getSetting() {
        return mDeviceStore.getSetting();
    }

    @Override
    public void controlGroup(int id, boolean status) {
        super.controlGroup(id, status);
    }


    private CustomDialog.Builder ibuilder = null;

    public CustomDialog.Builder Tips(String title, String msg) {
        ibuilder = new CustomDialog.Builder(MainActivity.this);
        ibuilder.setTitle(title);
        ibuilder.setMessage(msg);
        return ibuilder;
    }

    @Override
    public void jumpActivity(Class context, Bundle bundle) {
        if (MessageService.CURRENT_NETWORK_TYPE == MessageService.CONN_NETWORK_TYPE_NONE) {
            Tips(getString(R.string.tips), getString(R.string.connect_to_network)).setPositiveButton(R.string.yes, null).create().show();
        } else {
            Intent intent = new Intent(this, context);
            if (bundle != null) {
                intent.putExtra("bundle", bundle);
            }
            startActivity(intent);
        }
    }

    @Override
    public int getMobileNetworkState() {
        return MessageService.CURRENT_NETWORK_TYPE;
    }

    Runnable resetRunnable = new Runnable() {
        @Override
        public void run() {
            progressBar.dismiss();
            deviceListListener.refresh();
        }
    };

    @Override
    public void removeDevice(DeviceInfo info) {  //移除设备，多少秒之内进制操作
        mUUIDSERIAL.remove(info.getSerial());
        progressBar.setMessage(getString(R.string.remove_device));
        progressBar.show();
        if (info.getUuid() == null) {
            info.setUuid(devices.get(info.getSerial()).getUuid());
        }
        mDeviceStore.removeDeviceById(info.getId(), info.getSerial());
        HideDeviceInfo hide = new HideDeviceInfo();
        hide.setName(info.getName());
        hide.setSerial(info.getSerial());
        hide.setUuid(info.getUuid());
        hide.setMac(info.getMac());
        hide.setFactoryid(info.getDevsn());
        hide.setProductid(info.getDevtype());
        hide.setTime(System.currentTimeMillis());
        //取消订阅
        /*if (info.getUuid() != null) {
            Intent mIntent = new Intent();
            mIntent.setAction(MessageService.PHONE_CANCEL_MQTT_SUBSCRIBE);
            mIntent.putExtra("TOPICS", new String[]{info.getUuid() + "_bc"});
            sendBroadcast(mIntent);
        }*/
        devices.remove(info.getSerial());
        mDeviceStore.addHideDevice(hide);
        hideDevices.put(hide.getSerial(), hide);
        mainHandler.postDelayed(resetRunnable, 6 * 1000);
    }

    Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            if (remote_mode && phoneInfo.getUuid() != null && !"".equals(phoneInfo.getUuid())) {
                Map<String, String> head = new HashMap<>();
                head.put("wislink_auth_uuid", phoneInfo.getUuid());
                head.put("wislink_auth_token", phoneInfo.getToken());
                for (DeviceInfo info : devices.values()) {
                    if (info.getIP() == null && info.getUuid() != null && System.currentTimeMillis() - info.sendtime > 10 * 1000 && info.sendtime != 0) {
                        try {
                            String data = httpManagementAPI.getMethodGET(HTTPManagementAPI.WISLINK_URL + "status/" + info.getUuid(), head, true);
                            Log.e(TAG, "DATA: " + data);
                            JSONObject obj = new JSONObject(data);
                            deviceListListener.backUdpOnlineInquiry(getDeviceDevs(obj.getString("uuid")).getSerial(), obj.getBoolean("online"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                deviceListListener.querycarryout(true);
            } else {
                deviceListListener.querycarryout(false);
            }
        }
    };

    /**
     * 查询 APP 的版本号
     *
     * @return
     */
    @Override
    public String appVersion() {
        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS).versionName;
        } catch (Exception e) {
            e.printStackTrace();
            version = e.getMessage();
        }
        return version;
    }

    class MSGBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MessageService.PHONE_FIND_DEVICE_DROPPED)) {//本地模式掉线，
                String serial = intent.getStringExtra("json");
                if (devices.get(serial) != null) {
                    devices.get(serial).setIP(null);
                    devices.get(serial).online = false;
                }
                deviceListListener.updateDeviceStatus(serial, false, 2, 0, 0);
                if (statusListener != null) {
                    statusListener.deviceStatus(serial, false, false);
                }
                Intent mIntent = new Intent(MessageService.PHONE_REMOVE_LOCAL_SOCKET);
                mIntent.putExtra("SERIAL", serial);
                sendBroadcast(mIntent);
                if (devices.get(serial) != null) {
                    devices.get(serial).online = false;
                }
            } else if (action.equals(MessageService.DEVICE_BACK_PARAMEETER_SWVERSION)) {
                try {
                    JSONObject obj = new JSONObject(intent.getStringExtra("json"));
                    String serial;
                    if (obj.has("topic")) {
                        obj = obj.getJSONObject("data");
                        serial = mUUIDSERIAL.get(obj.getString("fromUuid"));
                    } else {
                        serial = obj.getString("devices");
                    }
                    obj = obj.getJSONObject("payload");
                    if (obj.has("swversion") && devices.get(serial) != null) {
                        devices.get(serial).swversion = obj.getString("swversion");
                        devices.get(serial).sendtime = 0;
                        queryDeviceState(devices.get(serial), 1);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (action.equals(MessageService.DEVICE_BACK_PARAMEETER_RESET)) {
                try {
                    JSONObject obj = new JSONObject(intent.getStringExtra("json"));
                    String serial;
                    if (obj.has("topic")) {
                        obj = obj.getJSONObject("data");
                        serial = mUUIDSERIAL.get(obj.getString("fromUuid"));
                    } else {
                        serial = obj.getString("devices");
                    }
                    obj = obj.getJSONObject("payload");
                    if (obj.has("resetfactory")) {
                        DeviceInfo receive = getDevice(serial);
                        if (receive == null) {
                            return;
                        }
                        //设备重置
                        if (obj.getInt("resetfactory") == 1) {
                            receive.online = false;
                            devices.get(receive.getSerial()).setIP(null);
                            devices.get(receive.getSerial()).online = false;
                            devices.get(receive.getSerial()).sendtime = 0;
                            devices.get(serial).online = false;
                            deviceListListener.updateDeviceStatus(receive.getSerial(), false, 2, 0, 0);

                        } else {
                            Log.e(TAG, "设备重置失败");
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(mTimerRequestDeviceState);

        stopService(new Intent("snail.digimagus.csrmesh.acplug"));
    }
}