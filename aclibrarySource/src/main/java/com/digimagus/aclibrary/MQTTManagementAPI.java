package com.digimagus.aclibrary;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ibm.micro.client.mqttv3.MqttCallback;
import com.ibm.micro.client.mqttv3.MqttClient;
import com.ibm.micro.client.mqttv3.MqttConnectOptions;
import com.ibm.micro.client.mqttv3.MqttDeliveryToken;
import com.ibm.micro.client.mqttv3.MqttException;
import com.ibm.micro.client.mqttv3.MqttMessage;
import com.ibm.micro.client.mqttv3.MqttTopic;
import com.ibm.micro.client.mqttv3.internal.MemoryPersistence;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 远程模式 MQTT
 */
public class MQTTManagementAPI {
    private final static String TAG = "MQTTManagementAPI";
    private Handler mqttHandler;
    private MqttClient mMqttClient;


    /**
     * 发送消息时不保存在数据库
     */
    public final static String MQTT_TOPIC_MESSAGE = "message";
    /**
     * 发送消息时  保存在数据库中
     */
    public final static String MQTT_TOPIC_DATA = "data";
    /**
     * MQTT 连接地址
     */
    public static final String MQTT_URL = "tcp://52.74.130.80:1883";
    /**
     * MQTT连接成功
     */
    public static final int MQTT_CONNECT_SUCCESS = 401;
    /**
     * MQTT连接失败
     */
    public static final int MQTT_CONNECT_FAILURE = 402;
    /**
     * MQTT消息发送成功
     */
    public static final int MQTT_MESSAGE_SEND_SUCCESS = 403;
    /**
     * MQTT消息发送失败
     */
    public static final int MQTT_MESSAGE_SEND_FAILURE = 404;
    /**
     * MQTT连接断开
     */
    public static final int MQTT_CONNECT_LOST = 405;
    /**
     * MQTT接收消息
     */
    public static final int MQTT_RECEIVE_MESSAGE = 406;

    public MQTTManagementAPI() {
    }

    /**
     * 单例模式
     *
     * MQTT 发送消息工具类
     */
    private static class LazyHolder {
        private static final MQTTManagementAPI INSTANCE = new MQTTManagementAPI();
    }
    public static final MQTTManagementAPI getInstance() {
        return LazyHolder.INSTANCE;
    }

    public ExecutorService fixedThreadPool = Executors.newSingleThreadExecutor();

    /**
     * MQTT 连接成功之后的回调函数
     */
    MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable throwable) {
            Message message = mqttHandler.obtainMessage();
            message.what = MQTT_CONNECT_LOST;
            mqttHandler.sendMessage(message);
        }

        //接收到消息 回调方法
        @Override
        public void messageArrived(MqttTopic mqttTopic, MqttMessage mqttMessage) throws Exception {
            Message message = mqttHandler.obtainMessage();
            message.what = MQTT_RECEIVE_MESSAGE;
            message.obj = new String(mqttMessage.getPayload());//返回接收的消息的内容
            mqttHandler.sendMessage(message);
        }

        @Override
        public void deliveryComplete(MqttDeliveryToken mqttDeliveryToken) {
        }
    };

    private String UUID,TOKEN;

    /**
     * 远程模式下 连接MQTT
     *
     * @param UUID    连接的帐号
     * @param TOKEN   连接的密码
     * @param TOPICS  订阅的消息
     * @param handler 消息发送器
     */
    public void connMqtt(final String UUID, final String TOKEN, final String[] TOPICS, Handler handler) {
        this.mqttHandler = handler;
        this.UUID=UUID;
        this.TOKEN=TOKEN;
        fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                subscribe(UUID, TOKEN, TOPICS, mqttCallback);
            }
        });
    }

    /**
     * 关闭MQTT 连接
     */
    public void disconnect() {
        try {
            if (mMqttClient != null) {
                mMqttClient.disconnect();
                mMqttClient=null;
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * MQTT 发送消息
     *
     * @param TOPIC       发送消息的类型[data(保存在服务器)、message(未保存服务器)]
     * @param sendMessage 发送的消息
     */
    public void publish(String TOPIC, String sendMessage) {
        if(mqttHandler!=null){
            Message message = mqttHandler.obtainMessage();
            try {
                if (TOPIC != null) {
                    MqttTopic mqttTopic = mMqttClient.getTopic(TOPIC);
                    MqttMessage mMqttMessage = new MqttMessage(sendMessage.getBytes());
                    mMqttMessage.setQos(1);
                    MqttDeliveryToken token = mqttTopic.publish(mMqttMessage);
                    while (!token.isComplete()) {
                        token.waitForCompletion(1000);
                    }
                    message.what = MQTT_MESSAGE_SEND_SUCCESS;
                }
            } catch (MqttException e) {
                e.printStackTrace();
                message.what = MQTT_MESSAGE_SEND_FAILURE;
            }
            mqttHandler.sendMessage(message);
        }
    }
    /**
     * @param uuid
     * @param token
     * @param topics
     * @throws MqttException
     */
    private void connect(String uuid, String token, String topics[]) throws MqttException {
        if(mMqttClient!=null){
            Log.e(TAG," -->subscribe   "+mMqttClient.isConnected());
        }
        if(mMqttClient==null||!mMqttClient.isConnected()){
            mMqttClient = new MqttClient(MQTT_URL, uuid.substring(0, 22), new MemoryPersistence());
            MqttConnectOptions conOptions = new MqttConnectOptions();
            conOptions.setCleanSession(false);
            conOptions.setUserName(uuid);
            conOptions.setPassword(token.toCharArray());
            conOptions.setConnectionTimeout(3*1000);
            conOptions.setKeepAliveInterval(20);
            mMqttClient.setCallback(mqttCallback);
            mMqttClient.connect(conOptions);
        }
        if(topics.length>0&&topics[0]!=null&&mMqttClient!=null){
            mMqttClient.subscribe(topics);
        }
    }

    /**
     * 订阅消息
     *
     * @param topics 订阅的主题（可变参数）
     * @throws MqttException 订阅消息抛出的异常信息
     */
    public void subscribe(String ...topics) throws MqttException {
        if(mMqttClient==null||!mMqttClient.isConnected()){
            connect(UUID, TOKEN, topics);
        } else if(mMqttClient.isConnected()){
            mMqttClient.subscribe(topics);
        }
    }

    /**
     * 取消订阅消息
     * @param topics 消息的主题
     * @throws MqttException 订阅消息 抛出的异常信息
     */
    public void unsubscribe(String... topics) throws MqttException {
        if(mMqttClient!=null&&mMqttClient.isConnected()){
            mMqttClient.unsubscribe(topics);
        }
    }

    /**
     * 判断MQTT 是否连接
     * 若连接就订阅消息
     * 未连接 先连接在订阅消息
     *
     * @param UUID         手机的UUDI标识
     * @param TOKEN        手机的密码标识
     * @param TOPICS       手机订阅的消息
     * @param mqttCallback 回调函数
     */
    public void subscribe(String UUID, String TOKEN, String[] TOPICS, MqttCallback mqttCallback) {
        Log.e(TAG, "UUID : " + UUID + "     TOKEN:" );
        if(mMqttClient!=null){
            Log.e(TAG," -->subscribe   "+mMqttClient.isConnected());
        }
        Message message = mqttHandler.obtainMessage();
        try {
            if (mMqttClient== null||!mMqttClient.isConnected()) {
                this.connect(UUID, TOKEN, TOPICS);
            }
            message.what = MQTT_CONNECT_SUCCESS;
        } catch (MqttException e) {
            e.printStackTrace();
            message.what = MQTT_CONNECT_FAILURE;
        }
        mqttHandler.sendMessage(message);
    }
}
