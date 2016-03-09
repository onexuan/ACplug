package com.digimagus.aclibrary;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.ibm.micro.client.mqttv3.MqttException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息 Service
 */
public class MessageService extends Service {
    private final static String TAG = "MessageService";

    private static class LazyHolder {
        private static final MessageService INSTANCE = new MessageService();
    }

    public static final MessageService getInstance() {
        return LazyHolder.INSTANCE;
    }

    public Map<String, LocalModel> mLocalModelMap = new ConcurrentHashMap<>();
    public Map<String, String> mUUIDSERIAL = new ConcurrentHashMap<>();
    public static String[] PHONEINFOMATION = new String[2];
    public static String[] TOPICS = null;

    /**
     * UDP 设备发现帧
     */
    private static final byte[] SEND_UDP_MESSAGE = new byte[]{0x55, (byte) 0xAA, 'A', 'I', 'I', 'P', 0x00, 0x00, 0x00, 0x00};
    private static final int UDP_8925 = 8925;
    private static final int UDP_28925 = 28925;

    private MessageBroadcastReceiver receiverMessage = new MessageBroadcastReceiver();
    private MessageHandler messageHandler = new MessageHandler();
    public final static String PHONE_REGISTER_SUCCESS = "PHONE_REGISTER_SUCCESS";//手机注册成功
    public final static String PHONE_REQUEST_CONN_MQTT = "PHONE_REGISTER_SUCCESS";//手机注册成功

    public final static String USER_REGISTER_PHONE = "USER_REGISTER_PHONE";//用户注册手机
    public final static String PHONE_LOCAL_SEND_MESSAGE = "PHONE_LOCAL_SEND_MESSAGE";//手机本地发送消息
    public final static String PHONE_REMOTE_SEND_MESSAGE = "PHONE_REMOTE_SEND_MESSAGE";//手机远程发送消息
    public final static String PHONE_CONN_MQTT_SERVER = "PHONE_CONN_MQTT_SERVER";//手机连接MQTT服务器
    public final static String PHONE_CONN_MQTT_SUCCESS = "PHONE_CONN_MQTT_SUCCESS";//手机连接MQTT成功
    public final static String PHONE_SENDMSG_MQTT_FAIL = "PHONE_SENDMSG_MQTT_FAIL";//手机MQTT消息发送失败
    public final static String PHONE_SET_SUCCESS = "";

    public final static String PHONE_SUBSCRIBE_MQTT_MESSAGE = "PHONE_SUBSCRIBE_MQTT_MESSAGE";//手机订阅MQTT消息
    public final static String PHONE_CANCEL_MQTT_SUBSCRIBE = "PHONE_CANCEL_MQTT_SUBSCRIBE";
    public final static String PHONE_FIND_DEVICE_DROPPED = "PHONE_FIND_DEVICE_DROPPED";//发现设备掉线
    public final static String PHONE_REMOVE_LOCAL_SOCKET = "PHONE_REMOVE_LOCAL_SOCKET";//发现设备掉线

    public final static String DEVICE_SEND_STATE = "DEVICE_SEND_STATE";//设备上报状态 1 查询状态 2 AP列表  3WiFi版本号  4定时配置 5倒计时

    /**
     * 手机当前连接的网络类型
     */
    public static int CURRENT_NETWORK_TYPE = MessageService.CONN_NETWORK_TYPE_NONE;
    public final static int CONN_NETWORK_TYPE_NONE = 10000;//没有网络
    public final static int CONN_NETWORK_TYPE_WIFI = 100001;//WiFI网络
    public final static int CONN_NETWORK_TYPE_MOBILE = 100002;//手机网络
    public static boolean CONN_NETWORK_IS_SERVER =false;//是否能连接服务器

    public final static String PHONE_NETWORK_TYPE = "PHONE_NETWORK_TYPE";//手机网络类型
    public final static String PHONE_FIND_DEVICE_UDP = "PHONE_FIND_DEVICE_UDP";

    private LocalModeManage mLocalModeManage = LocalModeManage.getInstance();
    private MQTTManagementAPI mMqttManagementAPI = MQTTManagementAPI.getInstance();
    private HTTPManagementAPI mHTTPManagementAPI = HTTPManagementAPI.getInstance();


    public static DatagramSocket socket;
    public static DatagramPacket packet;

    /**
     * Socket 连接成功
     */
    public static final int SOCKET_CONN_SUCCESS = 102;
    /**
     * Socket 连接失败
     */
    public static final int SOCKET_CONN_FAILURE = 103;

    /**
     * UDP 发现帧
     */
    public static final int UDP_DELAYED_5000 = 5000;
    public static final int FIND_DEVICE_DELAYED = 2 * 1000;
    public static final int FIND_DEVICE_DELAYED_2000 = 2 * 1000;
    public static final int QUERY_DELAYED_5000 = 15000;
    /**
     * 本地模式下心跳消息返回
     */
    public static final byte[] READ_SOCKET_HEART = new byte[]{0x55, (byte) 0xAA, 0x05, 0x00, 0x00, (byte) 0xA5, (byte) 0xAA};
    /**
     * 本地模式下 发送心跳消息
     */
    public static final byte[] SEND_SOCKET_HEART = new byte[]{0x55, (byte) 0xAA, 0x04, 0x00, 0x01, 0x05};

    /**
     * 设置WiFi切换时长 重启手机WiFi
     */
    public static final int REBOOT_WIFI = 20 * 1000;

    /**
     * 保存数据 延时时间
     */
    public static final int SAVE_DATA = 2 * 1000;

    /**
     * 开始启动连接
     */
    public static final int START_CONNECTION = 53;
    /**
     * 发现设备失败
     */
    public static final int FIND_DEVICE_FAILURE = 100;
    /**
     * UDP 读取成功
     */
    public static final int UDP_READ_SUCCESS = 101;

    /**
     * 消息发送成功
     */
    public static final int SOCKET_MESSAGE_SUCCESS = 104;
    /**
     * 消息发送失败
     */
    public static final int PING_NETWORK_FAILURE = 105;
    /**
     * Socket断开
     */
    public static final int SOCKET_CONN_DISCONNECT = 106;
    /**
     * 设备返回心跳消息
     */
    public static final int DEVICE_BACK_HEARTBEAT = 107;
    /**
     * 设备返回参数
     */
    public static final int DEVICE_BACK_PARAMETER = 108;
    /**
     * 设备设置返回参数
     */
    public static final int DEVICE_BACK_SETPARAMETER = 109;
    /**
     * 设备未知参数
     */
    public static final int DEVICE_BACK_UNKONW = 110;

    /**
     * 本地模式消息 头部命令段
     */
    public static final byte[] HEADER = {0x55, (byte) 0xAA};

    /**
     * 手机端定时发送心跳包保持TCP连接
     */
    public static final int SEND_HEARTBEAT_PACKAGE = 0x00;
    /**
     * 设备接收到心跳包后返回心跳应答
     */
    public static final int RECEIVE_HEARTBEAT_PACKAGE = 0x01;
    /**
     * 手机端获取参数
     */
    public static final int READ_PARAMETER_PACKAGE = 0x02;
    /**
     * 设备端接收到读取参数命令后返回
     */

    public static final int RECEIVE_MESSAGE_PACKAGE = 0x03;
    /**
     * 手机端发送设置命令格式
     */
    public static final int SET_PARAMETER_PACKAGE = 0x81;
    /**
     * 设备端执行设置后返回
     */
    public static final int SET_PARAMETER_BACK = 0x82;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    ReceiveUDPThread receiveUDPThread = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "-->onCreate");
        IntentFilter filter = new IntentFilter();
        filter.addCategory("receiver");
        filter.addAction(PHONE_LOCAL_SEND_MESSAGE);
        filter.addAction(PHONE_REMOVE_LOCAL_SOCKET);
        filter.addAction(PHONE_REMOTE_SEND_MESSAGE);
        filter.addAction(PHONE_CONN_MQTT_SERVER);
        filter.addAction(PHONE_SUBSCRIBE_MQTT_MESSAGE);
        filter.addAction(PHONE_CANCEL_MQTT_SUBSCRIBE);
        filter.addAction(USER_REGISTER_PHONE);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiverMessage, filter);

        try {
            socket = new DatagramSocket();
            packet = new DatagramPacket(SEND_UDP_MESSAGE, SEND_UDP_MESSAGE.length, InetAddress.getByName("255.255.255.255"), UDP_8925);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final static String DEVICE_BACK_PARAMEETER_RESULT = "DEVICE_BACK_PARAMEETER_RESULT";//返回设置的结果
    public final static String DEVICE_BACK_PARAMEETER_FAILE = "DEVICE_BACK_PARAMEETER_FAILE";//设备返回参数失败
    public final static String DEVICE_BACK_PARAMEETER_RESET = "DEVICE_BACK_PARAMEETER_RESET";//设备重置结果
    public final static String DEVICE_BACK_PARAMEETER_COUNTDOWN = "DEVICE_BACK_PARAMEETER_COUNTDOWN";//设备倒计时
    public final static String DEVICE_BACK_PARAMEETER_SWVERSION = "DEVICE_BACK_PARAMEETER_SWVERSION";//插座的版本
    public final static String DEVICE_BACK_PARAMEETER_SCHEDULE = "DEVICE_BACK_PARAMEETER_SCHEDULE";//插座的Schedule
    public final static String DEVICE_BACK_PARAMEETER_SWITCH1 = "DEVICE_BACK_PARAMEETER_SWITCH1";//插座的Switch1
    public final static String DEVICE_BACK_PARAMEETER_SWITCH2 = "DEVICE_BACK_PARAMEETER_SWITCH2";//插座的Switch2
    public final static String DEVICE_REMOTE_OFFLINE = "DEVICE_REMOTE_OFFLINE";//远程模式下设备掉线

    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Intent intent = new Intent();
            switch (msg.what) {
                case MQTTManagementAPI.MQTT_RECEIVE_MESSAGE: {//远程模式下MQTT接收消息
                    try {
                        JSONObject obj = new JSONObject(String.valueOf(msg.obj));
                        Log.e(TAG, "远程消息: " + obj);
                        if (obj.has("topic")) {
                            obj = obj.getJSONObject("data");
                            obj = obj.getJSONObject("payload");
                            if (obj.has("uuid") || obj.has("fromUuid")) {
                                Log.e(TAG, "格式错误：  " + obj);
                                return;
                            }
                            if (obj.has("resetfactory")) {//重置消息
                                intent.setAction(DEVICE_BACK_PARAMEETER_RESET);
                            } else if (obj.has("countdown")) {//倒计时
                                intent.setAction(DEVICE_BACK_PARAMEETER_COUNTDOWN);
                            } else if (obj.has("swversion")) {//插座的版本
                                intent.setAction(DEVICE_BACK_PARAMEETER_SWVERSION);
                            } else if (obj.has("schedule")) {//插座的Schedule
                                intent.setAction(DEVICE_BACK_PARAMEETER_SCHEDULE);
                            } else if (obj.has("switch") && obj.has("IRMS")) {//插座的switch IRMS
                                intent.setAction(DEVICE_BACK_PARAMEETER_SWITCH2);
                            } else if (obj.has("switch")) {//插座的switch
                                intent.setAction(DEVICE_BACK_PARAMEETER_SWITCH1);
                            } else if (obj.has("online")) {
                                intent.setAction(DEVICE_REMOTE_OFFLINE);
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case DEVICE_BACK_PARAMETER: {//本地模式下收到设备的消息
                    try {
                        JSONObject obj = new JSONObject(String.valueOf(msg.obj));
                        Log.e(TAG, "本地消息: " + obj);
                        obj = obj.getJSONObject("payload");
                        if (obj.has("resetfactory")) {//重置消息
                            intent.setAction(DEVICE_BACK_PARAMEETER_RESET);
                        } else if (obj.has("countdown")) {//倒计时
                            intent.setAction(DEVICE_BACK_PARAMEETER_COUNTDOWN);
                        } else if (obj.has("swversion")) {//插座的版本
                            intent.setAction(DEVICE_BACK_PARAMEETER_SWVERSION);
                        } else if (obj.has("schedule")) {//插座的Schedule
                            intent.setAction(DEVICE_BACK_PARAMEETER_SCHEDULE);
                        } else if (obj.has("switch") && obj.has("IRMS")) {//插座的switch IRMS
                            intent.setAction(DEVICE_BACK_PARAMEETER_SWITCH2);
                        } else if (obj.has("switch")) {//插座的switch
                            intent.setAction(DEVICE_BACK_PARAMEETER_SWITCH1);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        intent.setAction(DEVICE_BACK_PARAMEETER_FAILE);
                    }
                    break;
                }
                case MQTTManagementAPI.MQTT_CONNECT_SUCCESS: {//MQTT 连接成功
                    Log.e(TAG, "MQTT 连接成功");
                    intent.setAction(PHONE_CONN_MQTT_SUCCESS);
                    connmqtt = true;
                    break;
                }
                case MQTTManagementAPI.MQTT_CONNECT_FAILURE: {//MQTT 连接失败
                    Log.e(TAG, "MQTT 连接失败");
                    postDelayed(mConnMqtt, 5000);
                    connmqtt = false;
                    break;
                }
                case MQTTManagementAPI.MQTT_CONNECT_LOST: {//MQTT 连接断开
                    Log.e(TAG, "MQTT 连接断开");
                    postDelayed(mConnMqtt, 5000);
                    connmqtt = false;
                    break;
                }
                case MQTTManagementAPI.MQTT_MESSAGE_SEND_SUCCESS: {
                    Log.e(TAG, "MQTT 消息发送成功");
                    break;
                }
                case MQTTManagementAPI.MQTT_MESSAGE_SEND_FAILURE: {
                    Log.e(TAG, "MQTT 消息发送失败");
                    if (serial != null) {
                        intent.setAction(PHONE_SENDMSG_MQTT_FAIL);
                        intent.putExtra("serial", serial);
                    }
                    break;
                }
                case DEVICE_BACK_SETPARAMETER: {//设置参数返回
                    intent.setAction(DEVICE_BACK_PARAMEETER_RESULT);
                    break;
                }
                case SendMessageSocket.LOCAL_DROPPED: {//本地模式掉线
                    Log.e(TAG, "本地模式掉线 : " + String.valueOf(msg.obj));
                    mLocalModelMap.remove(String.valueOf(msg.obj));
                    intent.setAction(PHONE_FIND_DEVICE_DROPPED);
                    mLocalModeManage.removeDevice(String.valueOf(msg.obj));
                    break;
                }
            }
            if (intent.getAction() != null) {
                intent.putExtra("json", String.valueOf(msg.obj));
                sendBroadcast(intent);
            }
        }
    }

    Runnable mConnMqtt = new Runnable() {
        @Override
        public void run() {
            if (TOPICS != null && System.currentTimeMillis() - connmqttime >= 5 * 1000 && TOPICS.length > 0) {
                connmqttime = System.currentTimeMillis();
                mMqttManagementAPI.connMqtt(PHONEINFOMATION[0], PHONEINFOMATION[1], TOPICS, messageHandler);
            } else {
                Intent intent = new Intent(PHONE_REQUEST_CONN_MQTT);
                sendBroadcast(intent);
            }
        }
    };

    private long connmqttime = 0;

    /**
     * 发送UDP 广播
     */
    Runnable sendUDP = new Runnable() {
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (CURRENT_NETWORK_TYPE == CONN_NETWORK_TYPE_WIFI) {
                        try {
                            if (socket == null) {
                                socket = new DatagramSocket();
                                packet = new DatagramPacket(SEND_UDP_MESSAGE, SEND_UDP_MESSAGE.length, InetAddress.getByName("255.255.255.255"), UDP_8925);
                            }
                            socket.send(packet);
                            Log.e(TAG, "-------------------发送UDP  ");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            }).start();
            messageHandler.postDelayed(this, 3000);
        }
    };

    Runnable mRegisterRunnable = new Runnable() {
        @Override
        public void run() {
            String macAddress = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getMacAddress();
            Map<String, String> map = new HashMap<>();
            map.put("devsn", macAddress);
            map.put("devtype", "android");
            try {
                String json = mHTTPManagementAPI.getMethodPOST(HTTPManagementAPI.WISLINK_URL + "devices", map, null);
                Log.e(TAG, "register " + json);
                Intent intent = new Intent(PHONE_REGISTER_SUCCESS);
                intent.putExtra("json", json);
                sendBroadcast(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    class ReceiveUDPThread extends Thread {
        private boolean stop = false;

        @Override
        public void run() {
            super.run();
            try {
                MulticastSocket socket = new MulticastSocket(UDP_28925);
                while (!stop) {
                    byte data[] = new byte[70];
                    byte ip[] = new byte[4];
                    byte port[] = new byte[2];
                    byte mac[] = new byte[6];
                    byte serial[] = new byte[32];
                    byte firm[] = new byte[8];
                    byte type[] = new byte[8];
                    byte retention[] = new byte[8];
                    DatagramPacket inPacket = new DatagramPacket(data, data.length);
                    if (!stop && !socket.isClosed()) {
                        socket.receive(inPacket);
                        if (data[0] == (byte) 0x55 && data[1] == (byte) 0xAA) {
                            for (int i = 2; i < data.length; i++) {
                                if (i < 6) {
                                    ip[i - 2] = data[i];
                                } else if (i < 8) {
                                    port[i - 6] = data[i];
                                } else if (i < 14) {
                                    mac[i - 8] = data[i];
                                } else if (i < 46) {
                                    serial[i - 14] = data[i];
                                } else if (i < 54) {
                                    firm[i - 46] = data[i];
                                } else if (i < 62) {
                                    type[i - 54] = data[i];
                                } else if (i < 70) {
                                    retention[i - 62] = data[i];
                                }
                            }

                            String mSerial = new String(serial).trim();

                            LocalModel localModel = new LocalModel();
                            localModel.IP = (ip[0] & 0xFF) + "." + (ip[1] & 0xFF) + "." + (ip[2] & 0xFF) + "." + (ip[3] & 0xFF);
                            localModel.PORT = (data[7] << 8) + (data[6] & 0xFF);
                            localModel.MAC = Integer.toHexString(data[8] & 0xFF) + ":" + Integer.toHexString(data[9] & 0xFF) + ":" + Integer.toHexString(data[10] & 0xFF) + ":" + Integer.toHexString(data[11] & 0xFF) + ":" + Integer.toHexString(data[12] & 0xFF) + ":" + Integer.toHexString(data[13] & 0xFF);
                            localModel.SERIAL = mSerial;
                            localModel.firm = new String(firm).trim();
                            localModel.type = new String(type).trim();
                            localModel.retention = new String(retention).trim();


                            StringBuffer sb = new StringBuffer();
                            sb.append("{\"IP\":\"");
                            sb.append(localModel.IP);
                            sb.append("\",\"PROT\":");
                            sb.append(localModel.PORT);
                            sb.append(",\"MAC\":\"");
                            sb.append(localModel.MAC + "\"");
                            sb.append(",\"SERIAL\":\"" + localModel.SERIAL + "\"");
                            sb.append(",\"firm\":" + localModel.firm);
                            sb.append(",\"type\":" + localModel.type);
                            sb.append(",\"retention\":\"" + localModel.retention + "\"");
                            sb.append("}");


                            if (mLocalModelMap.get(mSerial) == null || System.currentTimeMillis() - mLocalModelMap.get(mSerial).time >= 3000) {
                                Intent intent = new Intent(PHONE_FIND_DEVICE_UDP);
                                intent.addCategory("receiver");
                                intent.putExtra("json", sb.toString());
                                sendBroadcast(intent);

                                localModel.time = System.currentTimeMillis();
                                mLocalModelMap.put(localModel.SERIAL, localModel);
                            }

                            Log.e(TAG, "Time:" + (System.currentTimeMillis() - mLocalModelMap.get(mSerial).time)+"    JSON:" + sb.toString() + "   ");
                        }
                    } else if (stop) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void stopThread() {
            stop = true;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "Message Service-->onDestroy");
        if (mLocalModeManage != null) {
            mLocalModeManage.stopSendMessage();
        }
        messageHandler.removeCallbacks(sendUDP);
        unregisterReceiver(receiverMessage);
    }

    public static boolean connmqtt = false;
    public String serial = null;


    class MessageBroadcastReceiver extends BroadcastReceiver {
        private long curren_time = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (action.equals(PHONE_LOCAL_SEND_MESSAGE)) {//手机本地发送消息
                String IP = intent.getStringExtra("IP");
                int PORT = intent.getIntExtra("PORT", 8803);
                serial = intent.getStringExtra("SERIAL");
                String DATA = intent.getStringExtra("DATA");
                byte STATE = intent.getByteExtra("STATE", (byte) 01);
                mLocalModeManage.sendMessage(IP, PORT, serial, DATA, STATE, messageHandler);
                Log.e(TAG, "发送本地Msg : " + DATA);
            } else if (action.equals(PHONE_REMOTE_SEND_MESSAGE)) {//手机远程发送消息
                serial = intent.getStringExtra("SERIAL");
                String UUID = intent.getStringExtra("UUID");
                final String DATA = intent.getStringExtra("DATA");
                final String MSGTYPE = intent.getStringExtra("MSGTYPE");
                Log.e(TAG, serial + " - 发送远程Msg : " + DATA);
                if (UUID != null) {
                    mUUIDSERIAL.put(UUID, serial);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mMqttManagementAPI.publish(MSGTYPE, DATA);
                        }
                    }).start();
                }
            } else if (action.equals(PHONE_CONN_MQTT_SERVER)) {//连接MQTT
                PHONEINFOMATION[0] = intent.getStringExtra("PHONEUUID");
                PHONEINFOMATION[1] = intent.getStringExtra("PHONETOKEN");
                TOPICS = intent.getStringArrayExtra("TOPICS");
                if (connmqtt) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mMqttManagementAPI.subscribe(TOPICS);
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    messageHandler.postDelayed(mConnMqtt, 3 * 1000);
                }
            } else if (action.equals(PHONE_SUBSCRIBE_MQTT_MESSAGE)) {//手机订阅MQTT消息
                TOPICS = intent.getStringArrayExtra("TOPICS");
                if (connmqtt) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mMqttManagementAPI.subscribe(TOPICS);
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    PHONEINFOMATION[0] = intent.getStringExtra("PHONEUUID");
                    PHONEINFOMATION[1] = intent.getStringExtra("PHONETOKEN");

                    Intent mIntent = new Intent(MessageService.PHONE_CONN_MQTT_SERVER);
                    sendBroadcast(mIntent);
                }

            } else if (action.equals(PHONE_CANCEL_MQTT_SUBSCRIBE)) {
                TOPICS = intent.getStringArrayExtra("TOPICS");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mMqttManagementAPI.unsubscribe(TOPICS);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else if (action.equals(PHONE_REMOVE_LOCAL_SOCKET)) {
                String SERIAL = intent.getStringExtra("SERIAL");
                mLocalModeManage.removeDevice(SERIAL);
            } else if (action.equals(USER_REGISTER_PHONE)) {
                new Thread(mRegisterRunnable).start();
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) && System.currentTimeMillis() - curren_time > 2 * 1000) {
                curren_time = System.currentTimeMillis();
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo == null) {
                    Log.e(TAG, "TAG ->  无网络连接");
                    messageHandler.removeCallbacks(sendUDP);
                    if (receiveUDPThread != null) {
                        receiveUDPThread.stopThread();
                    }
                } else if (netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    Log.e(TAG, "TAG ->  MOBILE网络连接");
                    messageHandler.removeCallbacks(sendUDP);
                    if (receiveUDPThread != null) {
                        receiveUDPThread.stopThread();
                    }
                } else if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    Log.e(TAG, "MessageBroadcastReceiver  TAG ->  WiFi网络连接");
                    receiveUDPThread = new ReceiveUDPThread();
                    messageHandler.post(sendUDP);
                    receiveUDPThread.start();
                } else {
                    Log.e(TAG, "未知网络");
                    messageHandler.removeCallbacks(sendUDP);
                    if (receiveUDPThread != null) {
                        receiveUDPThread.stopThread();
                    }
                }
            }
        }
    }
}
