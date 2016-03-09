package digimagus.csrmesh.acplug;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.digimagus.aclibrary.HTTPManagementAPI;
import com.digimagus.aclibrary.MQTTManagementAPI;
import com.digimagus.aclibrary.MessageService;
import com.digimagus.aclibrary.WiFiManagementAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.GroupDevice;
import digimagus.csrmesh.entities.HideDeviceInfo;
import digimagus.csrmesh.entities.PhoneInfo;

/**
 * 基类Activity
 */
public abstract class BaseActivity extends Activity {
    public final static String TAG = "BaseActivity_TAG";
    public static DeviceStore mDeviceStore;
    public static PhoneInfo phoneInfo;
    public static HTTPManagementAPI httpManagementAPI;
    public static WiFiManagementAPI wiFiManagementAPI;
    GroupDevice group;
    public MSGBroadcastReceiver receiver = new MSGBroadcastReceiver();
    //实时更新UI 数据的map集合
    public static Map<String, DeviceInfo> devices = new ConcurrentSkipListMap<>();
    //所有的组，以及组中的设备
    public static Map<Integer, GroupDevice> mGroupDevices = new ConcurrentSkipListMap<>();

    //UDP 扫描或者隐藏的设备
    public static Map<String, HideDeviceInfo> hideDevices = new ConcurrentSkipListMap<>();

    public static Map<String, String> mUUIDSERIAL = new ConcurrentHashMap<>();

    //让子类处理消息
    protected abstract void handler(Message msg);

    UIHandler handler = new UIHandler(Looper.getMainLooper());

    private void setHandler() {
        handler.setHandler(new IHandler() {
            public void handleMessage(Message msg) {
                handler(msg);//有消息就提交给子类实现的方法
            }
        });
    }

    Runnable bindingDevice = new Runnable() {
        @Override
        public synchronized void run() {
            if (!"".equals(phoneInfo.getUuid())) {
                Map<String, String> head = new HashMap<>();
                head.put("wislink_auth_uuid", phoneInfo.getUuid());
                head.put("wislink_auth_token", phoneInfo.getToken());
                Map<String, String> map2 = new HashMap<>();
                map2.put("uuid", phoneInfo.getUuid());
                for (DeviceInfo info : devices.values()) {
                    Log.e(TAG, "绑定设备...serial:" + info.getSerial() + "  uuid:" + info.getUuid());
                    if (info.getUuid() == null && info.getSerial() != null) {
                        try {
                            map2.put("devsn", info.getSerial());
                            map2.put("devtype", info.getDevsn() + info.getDevtype());
                            String bingdInfo = httpManagementAPI.getMethodPUT(HTTPManagementAPI.WISLINK_URL + "mbclaimdevice", head, map2);
                            Log.e(TAG, "BD  ->   " + bingdInfo);
                            JSONObject obj = new JSONObject(bingdInfo);
                            DeviceInfo deviceInfo = mDeviceStore.getDeviceInfo(info.getSerial());
                            info.setSerial(info.getSerial());
                            info.setUuid(obj.getString("uuid"));
                            info.setDevtype(obj.getString("devtype"));
                            mDeviceStore.addDeviceInfo(deviceInfo);
                            if (mUUIDSERIAL.get(info.getUuid()) == null) {
                                Intent mIntent = new Intent(MessageService.PHONE_SUBSCRIBE_MQTT_MESSAGE);
                                mIntent.putExtra("PHONEUUID", phoneInfo.getUuid());
                                mIntent.putExtra("PHONETOKEN", phoneInfo.getToken());
                                mIntent.putExtra("TOPICS", new String[]{info.getUuid() + "_bc"});
                                sendBroadcast(mIntent);
                                mUUIDSERIAL.put(info.getUuid(), info.getSerial());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onPause() {
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHandler();
        initContentView(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory("receiver");
        intentFilter.addAction(MessageService.PHONE_FIND_DEVICE_UDP);//手机发现设备的UDP广播
        intentFilter.addAction(MessageService.DEVICE_BACK_PARAMEETER_SWVERSION);//设备返回firmware版本号
        intentFilter.addAction(MessageService.DEVICE_BACK_PARAMEETER_SWITCH1);//设备返回开关状态1
        intentFilter.addAction(MessageService.DEVICE_BACK_PARAMEETER_SWITCH2);//设备返回开关状态2
        intentFilter.addAction(MessageService.DEVICE_BACK_PARAMEETER_RESET);//设备返回重置
        intentFilter.addAction(MessageService.PHONE_FIND_DEVICE_DROPPED);//手机发现设备掉线
        intentFilter.addAction(MessageService.PHONE_REGISTER_SUCCESS);//手机注册成功
        intentFilter.addAction(MessageService.DEVICE_BACK_PARAMEETER_SCHEDULE);//设备返回schedule 参数
        intentFilter.addAction(MessageService.DEVICE_BACK_PARAMEETER_COUNTDOWN);//设备返回 AC Plug 倒计时
        intentFilter.addAction(MessageService.DEVICE_BACK_PARAMEETER_RESULT);//设备返回设置参数的结果
        intentFilter.addAction(MessageService.PHONE_CONN_MQTT_SUCCESS);//手机连接MQTT 成功
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(MessageService.PHONE_SENDMSG_MQTT_FAIL);
        intentFilter.addAction(MessageService.DEVICE_REMOTE_OFFLINE);
        intentFilter.addAction(MessageService.PHONE_REQUEST_CONN_MQTT);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDeviceStore == null) {
            mDeviceStore = DeviceStore.getInstance(this);//实例化
            mDeviceStore.loadAllInfo();//读取配置

            devices = mDeviceStore.getDevices();//得到设备新
            mGroupDevices = mDeviceStore.getAllGroups();//得到组信息
            hideDevices = mDeviceStore.getHideDevices();//得到隐藏的设备
            phoneInfo = mDeviceStore.getPhoneInfo();//得到手机注册的信息

            Log.e(TAG, "phoneInfo:  " + phoneInfo.getUuid() + "     " + phoneInfo.getToken());

            wiFiManagementAPI = WiFiManagementAPI.getInstance(this);
            httpManagementAPI = HTTPManagementAPI.getInstance();


        }
    }


    // 初始化UI，setContentView等
    abstract void initContentView(Bundle savedInstanceState);

    class UIHandler extends Handler {
        private IHandler handler;//回调接口，消息传递给注册者

        public UIHandler(Looper looper) {
            super(looper);
        }

        public void setHandler(IHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (handler != null) {
                handler.handleMessage(msg);//有消息，就传递
            }
        }
    }

    public interface UpdateDeviceStateListener {
        void updateDeviceState(String serial);
    }

    public static UpdateDeviceStateListener listener;

    public void setUpdateDeviceState(UpdateDeviceStateListener listener) {
        this.listener = listener;
    }

    public interface IHandler {
        void handleMessage(Message msg);
    }

    public static final int PHONE_FIND_DEVICE = 1000;
    public static final int DEVICE_BACK_VERSION = 1001;
    public static final int DEVICE_BACK_SCHEDULE = 1002;
    public static final int DEVICE_BACK_COUNTDOWN = 1003;
    public static final int PHONE_SET_PARAMEETER_RESULT = 1004;
    public static final int DEVICE_BACK_STATE1 = 1005;
    public static final int NOTYFY_UI_CHANGE = 1007;
    long network_change_time = 0;

    Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            if (wiFiManagementAPI.ping("github.com")) {
                MessageService.CONN_NETWORK_IS_SERVER = true;
                Log.e(TAG,"网络 ping 通....... ");

            } else {
                MessageService.CONN_NETWORK_IS_SERVER = false;
                Log.e(TAG,"网络 ping 不通....... ");
            }
        }
    };

    class MSGBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {//监听网络状态变化
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (System.currentTimeMillis() - network_change_time > 3 * 1000) {
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

                    new Thread(pingRunnable).start();

                }
                network_change_time = System.currentTimeMillis();
            } else if (action.equals(MessageService.PHONE_CONN_MQTT_SUCCESS)) {//Phone conn mqtt success
                Intent mIntent = new Intent(MessageService.PHONE_SUBSCRIBE_MQTT_MESSAGE);
                List<String> mTopics = new ArrayList<>();
                for (DeviceInfo d : devices.values()) {
                    if (d.getUuid() != null) {
                        mUUIDSERIAL.put(d.getUuid(), d.getSerial());
                        mTopics.add(d.getUuid() + "_bc");
                    }
                }
                String[] TOPICS = new String[mTopics.size()];

                mIntent.putExtra("TOPICS", mTopics.toArray(TOPICS));
                sendBroadcast(mIntent);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (String s : mUUIDSERIAL.values()) {
                            queryDeviceState(devices.get(s), 1);
                        }
                    }
                }).start();
            } else if (action.equals(MessageService.PHONE_REGISTER_SUCCESS)) {//phone register success
                try {
                    String data = intent.getStringExtra("json");
                    if (data != null) {
                        JSONObject obj = new JSONObject(data);
                        phoneInfo.setMac(obj.getString("devsn"));
                        phoneInfo.setType(obj.getString("devtype"));
                        phoneInfo.setUuid(obj.getString("uuid"));
                        phoneInfo.setToken(obj.getString("token"));

                        Log.e(TAG, "PHONE2 - UUID:  " + phoneInfo.getUuid() + "  TOKEN  " + phoneInfo.getToken());
                        mDeviceStore.updatePhoneInfo(phoneInfo);
                        new Thread(bindingDevice).start();
                        /***
                         * 手机注册成功
                         * 绑定设备
                         * 连接MQTT Server
                         */
                        connMQTTServer();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (action.equals(MessageService.DEVICE_BACK_PARAMEETER_SWITCH2)) {//设备的开关状态消息2
                try {
                    JSONObject obj = new JSONObject(intent.getStringExtra("json"));
                    /*更新设备的状态*/
                    String serial;
                    if (obj.has("topic")) {
                        obj = obj.getJSONObject("data");
                        serial = mUUIDSERIAL.get(obj.getString("fromUuid"));
                    } else {
                        serial = obj.getString("devices");
                    }

                    Log.e(TAG, serial + "- DATA- " + obj);

                    if (serial != null) {
                        obj = obj.getJSONObject("payload");
                        if (obj.has("switch") && obj.has("IRMS")) {
                            if (devices.get(serial) != null) {
                                devices.get(serial).remaining = obj.getInt("remaining");
                                devices.get(serial).power = obj.getDouble("Power") / 100;
                                devices.get(serial).online = true;
                                devices.get(serial).state = obj.getInt("switch");
                                devices.get(serial).activated = false;
                            }
                        }
                    /*更新组中设备的状态*/
                        for (GroupDevice group : mGroupDevices.values()) {
                            for (DeviceInfo device : group.getDevices().values()) {
                                if (serial.equals(device.getSerial())) {
                                    group.getDevices().put(serial, devices.get(serial));
                                    break;
                                }
                            }
                        }
                        for (Map.Entry<Integer, Map<String, DeviceInfo>> entry : sendMsgDevice.entrySet()) {
                            entry.getValue().remove(serial);
                            //Log.e(TAG, "TAG key:" + entry.getKey() + "   size:" + entry.getValue().size());
                            if (entry.getValue().size() == 0 && mGroupDevices.get(entry.getKey()) != null) {
                                mGroupDevices.get(entry.getKey()).activated = false;
                            }
                        }

                    /*通知界面改变*/
                        Message msg = handler.obtainMessage();
                        msg.what = NOTYFY_UI_CHANGE;
                        msg.obj = serial;
                        handler.sendMessage(msg);
                        if (listener != null) {
                            listener.updateDeviceState(serial);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (action.equals(MessageService.PHONE_SENDMSG_MQTT_FAIL)) {
                String serial = intent.getStringExtra("serial");
                if (listener != null) {
                    listener.updateDeviceState(serial);
                }
            } else if (action.equals(MessageService.DEVICE_BACK_PARAMEETER_RESET)) {//设备重置
                try {
                    JSONObject obj = new JSONObject(intent.getStringExtra("json"));
                    String serial;
                    if (obj.has("topic")) {
                        obj = obj.getJSONObject("data");
                        serial = mUUIDSERIAL.get(obj.getString("fromUuid"));
                    } else {
                        serial = obj.getString("devices");
                    }
                    if (serial != null) {
                        if (listener != null && devices.get(serial) != null) {
                            devices.get(serial).setIP(null);
                            devices.get(serial).online = false;
                            listener.updateDeviceState(serial);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (action.equals(MessageService.PHONE_FIND_DEVICE_DROPPED)) {//设备dropped
                String serial = intent.getStringExtra("json");
                Log.e(TAG, "本地模式掉线:  " + serial);
                if (listener != null && devices.get(serial) != null) {
                    devices.get(serial).online = false;
                    devices.get(serial).setIP(null);
                    listener.updateDeviceState(serial);
                }
            } else if (action.equals(MessageService.DEVICE_REMOTE_OFFLINE)) {
                try {
                    JSONObject obj = new JSONObject(intent.getStringExtra("json"));
                    String serial = mUUIDSERIAL.get(obj.getJSONObject("data").getString("fromUuid"));
                    Log.e(TAG, serial + "远程模式掉线:  " + obj);
                    if (listener != null && devices.get(serial) != null) {
                        devices.get(serial).online = false;
                        listener.updateDeviceState(serial);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (action.equals(MessageService.PHONE_REQUEST_CONN_MQTT)) {
                connMQTTServer();
            } else {
                Message message = handler.obtainMessage();
                String sendmsg = intent.getStringExtra("json");
                if (sendmsg == null) {
                    return;
                } else {
                    message.obj = sendmsg;
                }
                if (action.equals(MessageService.PHONE_FIND_DEVICE_UDP)) {//发现设备
                    message.what = PHONE_FIND_DEVICE;//发现UDP 设备
                    //Log.e(TAG, "Send Message : " + sendmsg);
                } else if (action.equals(MessageService.DEVICE_BACK_PARAMEETER_SWVERSION)) {//firmware version
                    message.what = DEVICE_BACK_VERSION;//设备版本号
                } else if (action.equals(MessageService.DEVICE_BACK_PARAMEETER_SCHEDULE)) {//设备返回schedule
                    message.what = DEVICE_BACK_SCHEDULE;
                } else if (action.equals(MessageService.DEVICE_BACK_PARAMEETER_COUNTDOWN)) {//设备返回timer
                    message.what = DEVICE_BACK_COUNTDOWN;
                } else if (action.equals(MessageService.DEVICE_BACK_PARAMEETER_RESULT)) {//设置参数返回结果
                    message.what = PHONE_SET_PARAMEETER_RESULT;
                } else if (action.equals(MessageService.DEVICE_BACK_PARAMEETER_SWITCH1)) {//设备的开关状态消息1
                    message.what = DEVICE_BACK_STATE1;
                    if (listener != null) {
                        listener.updateDeviceState(sendmsg);
                    }
                }
                handler.sendMessage(message);
            }
        }
    }

    public void setDeviceState(DeviceInfo info) {
        if (info.getUuid() == null) {
            info.setUuid(devices.get(info.getSerial()).getUuid());
        }
        if (info.getIP() == null) {
            info.setIP(devices.get(info.getSerial()).getIP());
        }
        devices.get(info.getSerial()).activated = true;
        String json = "{\"switch\":" + (info.state == 0 ? 1 : 0) + "}";
        info.msgtype = (byte) 0x81;
        sendMsg(info, json);
    }

    public void connMQTTServer() {
        List<String> mytopics = new ArrayList<>();
        Log.e(TAG, "connMQTTServer  -->   SIZE  :  " + devices.size());
        for (DeviceInfo d : devices.values()) {
            Log.e(TAG, "serial :  " + d.getSerial() + "  uuid: " + d.getUuid());
            if (d.getUuid() != null && mUUIDSERIAL.get(d.getUuid()) == null) {
                mytopics.add(d.getUuid() + "_bc");
            }
        }
        if (mytopics.size() > 0) {
            Intent mIntent = new Intent(MessageService.PHONE_CONN_MQTT_SERVER);
            mIntent.putExtra("PHONEUUID", phoneInfo.getUuid());
            mIntent.putExtra("PHONETOKEN", phoneInfo.getToken());
            String[] arr = new String[mytopics.size()];
            mIntent.putExtra("TOPICS", mytopics.toArray(arr));
            sendBroadcast(mIntent);
        } else {
            new Thread(bindingDevice).start();
        }
    }

    /**
     * 查询状态
     * 查询状态             1
     * AP列表               2
     * Wifi版本号           3
     * 定时配置             4
     * 查询设备的倒计时      5
     *
     * @param info
     * @param state 请求的状态命令
     */
    public void queryDeviceState(DeviceInfo info, int state) {
        if (info != null) {
            String json = "{\"querystatus\":" + state + "}";
            info.msgtype = 0x02;
            sendMsg(info, json);
        }
    }

    /**
     * 设置schedule  或者删除 schedule
     */
    public void setSchedule(String serial, String data) {
        DeviceInfo info = devices.get(serial);
        info.msgtype = (byte) 0x81;
        sendMsg(info, data);
        Log.e(TAG, "  " + data);
    }


    public void getDeviceTimer(String serial) {
        DeviceInfo info = devices.get(serial);
        if (info != null) {
            queryDeviceState(info, 5);
        }
    }

    public void setDeviceSystemTime(String serial) {
        Calendar calendar = Calendar.getInstance();
        DeviceInfo info = devices.get(serial);
        info.msgtype = (byte) 0x81;
        StringBuffer sb = new StringBuffer();
        sb.append("{\"systemtime\":{\"year\":");
        sb.append(calendar.get(Calendar.YEAR) - 2000);
        sb.append(",\"month\":");
        sb.append(calendar.get(Calendar.MONTH) + 1);
        sb.append(",\"day\":");
        sb.append(calendar.get(Calendar.DAY_OF_MONTH));
        sb.append(",\"week\":");
        sb.append(calendar.get(Calendar.DAY_OF_WEEK));
        sb.append(",\"hour\":");
        sb.append(calendar.get(Calendar.HOUR_OF_DAY));
        sb.append(",\"minute\":");
        sb.append(calendar.get(Calendar.MINUTE));
        sb.append(",\"second\":");
        sb.append(calendar.get(Calendar.SECOND));
        sb.append(",\"timezone\":");
        sb.append(timeZone());
        sb.append("}}");
        sendMsg(info, sb.toString());
    }

    //得到手机本地的时区
    public int timeZone() {
        TimeZone tz = TimeZone.getDefault();
        return Integer.parseInt(createGmtOffsetString(tz.getRawOffset()));
    }


    public static String createGmtOffsetString(int offsetMillis) {
        int offsetMinutes = offsetMillis / 60000;
        if (offsetMinutes < 0) {
            offsetMinutes = -offsetMinutes;
        }
        StringBuilder builder = new StringBuilder(9);
        appendNumber(builder, 2, offsetMinutes / 60);
        return builder.toString();
    }

    private static void appendNumber(StringBuilder builder, int count, int value) {
        String string = Integer.toString(value);
        for (int i = 0; i < count - string.length(); i++) {
            builder.append('0');
        }
        builder.append(string);
    }

    public void setDeviceTimer(String serial, String timer) {
        DeviceInfo info = devices.get(serial);
        if (info != null) {
            String json = "{\"countdown\":" + timer + "}";
            info.msgtype = (byte) 0x81;
            sendMsg(info, json);
        }
    }

    /**
     * 根据设置的不同消息 发送参数
     */
    public void sendMsg(DeviceInfo info, String json) {
        /**
         * 心跳发送是 0x00  心跳参数返回 0x01
         * 读取参数是 0x02  读取参数返回 0x03
         * 设置参数是 0x81  设置参数返回 0x82
         *       查询状态
         * a、查询状态      1
         * b、AP列表       2
         * c、Wifi版本号   3
         * d、定时配置     4
         * 1、发送开关命令
         * 2、查询设备状态命令
         * 3、查询设备版本命令
         *    设置时间
         * 4、查询Schedule 命令
         * 5、设置Schedule 命令
         * 6、删除Schedule 命令
         */
        //组合参数
        json = "{\"devices\":\"" + (info.getUuid() == null ? "*" : info.getUuid()) + "\",\"payload\":" + json + "}";
        info.msg = json;//设置发送的JSON 数据

        if (info.getIP() != null) {
            info.sendtime = System.currentTimeMillis();//设置当前时间
            Intent intent = new Intent(MessageService.PHONE_LOCAL_SEND_MESSAGE);
            intent.putExtra("IP", info.getIP());
            intent.putExtra("PORT", info.getPORT());
            intent.putExtra("SERIAL", info.getSerial());
            intent.putExtra("DATA", info.msg);
            intent.putExtra("STATE", info.msgtype);
            sendBroadcast(intent);
        } else if (System.currentTimeMillis() - info.sendtime > 1000/* && info.sendtime != 0*/) {
            if (info.getUuid() == null || "".equals(info.getUuid())) {
                new Thread(bindingDevice).start();
            } else {
                info.sendtime = System.currentTimeMillis();//设置当前时间
                Intent intent = new Intent(MessageService.PHONE_REMOTE_SEND_MESSAGE);
                intent.putExtra("SERIAL", info.getSerial());
                intent.putExtra("UUID", info.getUuid());
                intent.putExtra("DATA", info.msg);
                intent.putExtra("MSGTYPE", MQTTManagementAPI.MQTT_TOPIC_MESSAGE);
                sendBroadcast(intent);
            }
        }
    }


    public void addShowDevice(HideDeviceInfo hide) {
        mDeviceStore.removeHideDevice(hide);
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setName(hide.getName());
        deviceInfo.setDevsn(hide.getFactoryid());
        deviceInfo.setDevtype(hide.getProductid());
        deviceInfo.setSerial(hide.getSerial());
        deviceInfo.setUuid(hide.getUuid());
        deviceInfo.online = false;
        deviceInfo.setMac(hide.getMac());
        hideDevices.remove(hide.getSerial());//移除设备  移除数据库
        devices.put(hide.getSerial(), mDeviceStore.addDeviceInfo(deviceInfo));
        Log.e(TAG, "Name:" + hide.getName() + "  UUID:" + hide.getUuid() + "  Serial:" + hide.getSerial());

        if (hide.getUuid() != null && mUUIDSERIAL.get(hide.getUuid()) == null) {//重新订阅
            Log.e(TAG, "重新订阅    :" + hide.getSerial());
            Intent mIntent = new Intent(MessageService.PHONE_SUBSCRIBE_MQTT_MESSAGE);
            mIntent.putExtra("PHONEUUID", phoneInfo.getUuid());
            mIntent.putExtra("PHONETOKEN", phoneInfo.getToken());
            mIntent.putExtra("TOPICS", new String[]{hide.getUuid()});
            sendBroadcast(mIntent);
            mUUIDSERIAL.put(hide.getUuid(), hide.getSerial());
        }
    }

    public void getGDeviceSchedule(int groupId) {
        for (DeviceInfo info : mDeviceStore.getGroupDeviceById(groupId)) {
            queryDeviceState(info, 4);
        }
    }


    private ExecutorService pool = Executors.newFixedThreadPool(3);

    public void setPrivacy(boolean state) {
        privacySw = state;
        pool.execute(setprivacyRunnable);
    }

    public static boolean privacySw;
    public final static int SET_PRIVACY = 0x01;

    public void getPrivacy() {
        if (devices.size() > 0 && phoneInfo != null && !"".equals(phoneInfo.getUuid())) {
            pool.execute(getprivacyRunnable);
        } else {
            Message message = handler.obtainMessage();
            message.what = SET_PRIVACY;
            message.obj = privacySw;
            handler.sendMessage(message);
        }
    }

    Runnable getprivacyRunnable = new Runnable() {
        @Override
        public void run() {
            if (phoneInfo.getUuid() != null && !"".equals(phoneInfo.getUuid())) {
                Map<String, String> head = new HashMap<>();
                head.put("wislink_auth_uuid", phoneInfo.getUuid());
                head.put("wislink_auth_token", phoneInfo.getToken());
                List<Boolean> params = new ArrayList<>();
                //请求设备隐私
                for (DeviceInfo info : devices.values()) {
                    if (info.getUuid() != null && !"".equals(info.getUuid())) {
                        try {
                            String data = httpManagementAPI.getMethodGET(HTTPManagementAPI.WISLINK_URL + "getparams/" + info.getUuid(), head, true);
                            JSONObject obj = new JSONObject(data);
                            params.add(obj.getBoolean("privacy"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (params.size() == 0) {//隐私为false
                    privacySw = false;
                } else {// 全为true时   为 true  否则为false
                    for (Boolean bool : params) {
                        if (bool) {
                            privacySw = true;
                        } else {
                            privacySw = false;
                            break;
                        }
                    }
                }
            }

            Message message = handler.obtainMessage();
            message.what = SET_PRIVACY;
            message.obj = privacySw;
            handler.sendMessage(message);
        }
    };


    Runnable setprivacyRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "修改隐私： " + phoneInfo.getUuid() + "   " + phoneInfo.getToken());
            if (phoneInfo.getUuid() != null && !"".equals(phoneInfo.getUuid())) {
                try {
                    List<Boolean> params = new ArrayList<>();
                    boolean privacy = true;
                    Map<String, String> head = new HashMap<>();
                    head.put("wislink_auth_uuid", phoneInfo.getUuid());
                    head.put("wislink_auth_token", phoneInfo.getToken());
                    Map<String, String> map = new HashMap<>();
                    map.put("payload", "{\"privacy\":" + privacySw + "}");
                    for (DeviceInfo info : devices.values()) {
                        map.put("uuid", info.getUuid());
                        String data = httpManagementAPI.getMethodPOST(HTTPManagementAPI.WISLINK_URL + "setparams", map, head);
                        JSONObject obj = new JSONObject(data);
                        params.add(obj.getBoolean("privacy"));
                    }
                    if (params.size() == 0) {//隐私为false
                        privacy = false;
                    } else {// 全为true时   为 true  否则为false
                        for (Boolean bool : params) {
                            if (bool) {
                                privacy = true;
                            } else {
                                privacy = false;
                                break;
                            }
                        }
                    }
                    setPrivacy(privacy);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    List<DeviceInfo> sendGroup = new ArrayList<>();
    private boolean grouoState;

    private Map<Integer, Map<String, DeviceInfo>> sendMsgDevice = new ConcurrentHashMap<>();

    public void controlGroup(int id, boolean status) {
        Map<String, DeviceInfo> deviceInfoMap = new ConcurrentHashMap<>();

        for (DeviceInfo map : mGroupDevices.get(id).getDevices().values()) {
            deviceInfoMap.put(map.getSerial(), map);
        }

        sendMsgDevice.put(id, deviceInfoMap);

        this.grouoState = status;
        sendGroup = mDeviceStore.getGroupDeviceById(id);
        mGroupDevices.get(id).activated = true;
        new Thread(runnableGroup).start();
    }

    Runnable runnableGroup = new Runnable() {
        @Override
        public void run() {
            if (!sendGroup.isEmpty()) {
                for (DeviceInfo info : sendGroup) {
                    info = devices.get(info.getSerial());
                    info.state = grouoState ? 1 : 0;
                    setDeviceState(info);
                }
                sendGroup.clear();
            }
            // handler.postDelayed(this, 1000);
        }
    };
}
