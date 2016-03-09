package com.digimagus.aclibrary;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

/**
 * 单个 Socket 管理通信API
 */
public class SocketManagementAPI {

    public static final String TAG = "SocketManagementAPI";

    public static Socket SOCKET = null;

    public SendMessageThread sendThread;
    public ReceiveMessageThread receiveThread;


    private static class LazyHolder {
        private static final SocketManagementAPI INSTANCE = new SocketManagementAPI();
    }
    public static final SocketManagementAPI getInstance() {
        return LazyHolder.INSTANCE;
    }

    public SocketManagementAPI() {

    }


    /**
     * 本地模式 Socket 连接
     *    本地模式 Socket 根据IP 、PORT 连接
     * @param ip IP地址
     * @param port 端口号
     * @param handler handler 返回消息使用
     */
    public void conn(String ip, int port, Handler handler) {
        new ConnDeviceThread(ip, port, handler).start();
    }

    /**
     * 开启发送、接收消息的线程
     *    开始本地模式下 发送消息 接收消息的线程
     * @param handler handler 返回消息使用
     */
    public void startThread(Handler handler) {
        sendThread = new SendMessageThread(handler);
        receiveThread = new ReceiveMessageThread(SOCKET, handler);
        sendThread.start();
        receiveThread.start();
    }

    /**
     *关闭本地模式线程连接
     *
     */
    public void stopThread() {
        if (receiveThread != null||receiveThread.run) {
            receiveThread.stopMessage();
        }
        if (sendThread != null||sendThread.run) {
            sendThread.stopMessage();
        }
    }

    /**
     * 本地模式下 向设备端发送消息
     *
     * @param data  发送的byte 类型数据
     */
    public void sendMessage(byte[] data) {
        if(SOCKET!=null){
            sendThread.sendMessage(data, SOCKET);
        }
    }

    /**
     * 本地模式下 向设备端发送消息
     *   本地模式下向设备发送的字符串数据 转成 byte数组
     * @param d 发送数据字符串
     * @param cmd 发送的命令 [查询、设置]
     */
    public void sendMessage(String d, byte cmd) {
        Log.e(TAG,"本地模式发送数据  " +  d);
        if(d!=null){
            byte[] data = d.getBytes();
            byte[] command = {cmd};
            byte[] packet = {(byte) (data.length + 4 & 255), (byte) (data.length + 4 >> 8)};
            byte[] byteAll = LocalModeManage.byteMerger(packet, command, data);
            byte[] crc = new byte[]{LocalModeManage.getCrc8(byteAll, packet.length + 1 + data.length)};
            ByteBuffer buffer = ByteBuffer.allocate(2 + packet.length + 1 + data.length + crc.length);
            buffer.put(MessageService.HEADER);
            buffer.put(packet);
            buffer.put(command);
            buffer.put(data);
            buffer.put(crc);
            buffer.flip();
            sendMessage(buffer.array());
        }
    }

    class ConnDeviceThread extends Thread {
        private String IP;
        private int PORT;
        private Handler handler;

        public ConnDeviceThread(String ip, int port, Handler handler) {
            this.IP = ip;
            this.PORT = port;
            this.handler = handler;
        }

        @Override
        public void run() {
            super.run();
            Message message = handler.obtainMessage();
            try {
                if (SOCKET != null) {
                    SOCKET.close();
                }
                SOCKET = new Socket(IP, PORT);
                SOCKET.setSoTimeout(10 * 1000);
                if (SOCKET.isConnected()) {//连接成功
                    message.what = MessageService.SOCKET_CONN_SUCCESS;
                } else {
                    message.what = MessageService.SOCKET_CONN_FAILURE;
                }
            } catch (IOException e) {
                e.printStackTrace();
                message.what = MessageService.SOCKET_CONN_FAILURE;
            }
            handler.sendMessage(message);
        }
    }

    /**
     * 发送消息
     */
    class SendMessageThread extends Thread {
        public Socket socket;
        private boolean run = true;
        private byte[] msg = null;
        private Handler handler;

        public SendMessageThread(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            super.run();
            while (run) {
                if (msg != null) {
                    Message message = handler.obtainMessage();
                    try {
                        OutputStream os = socket.getOutputStream();
                        os.write(msg);
                        os.flush();
                        message.what = MessageService.SOCKET_MESSAGE_SUCCESS;
                    } catch (IOException e) {
                        e.printStackTrace();
                        message.what = MessageService.SOCKET_CONN_DISCONNECT;
                    }
                    msg = null;
                    handler.sendMessage(message);
                }
            }
        }

        /**
         * 发送消息
         *
         * @param msg
         */
        public void sendMessage(byte[] msg,Socket socket) {
            this.msg = msg;
            this.socket=socket;
        }

        public void stopMessage() {
            try {
                this.run = false;
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ReceiveMessageThread extends Thread {
        public Socket socket;
        private boolean run = true;
        private Handler handler;

        public ReceiveMessageThread(Socket socket, Handler handler) {
            this.socket = socket;
            this.handler = handler;
        }

        @Override
        public void run() {
            super.run();
            while (run) {
                Message message = handler.obtainMessage();
                try {
                    InputStream input = socket.getInputStream();
                    byte[] data1 = new byte[5];
                    byte[] data2 = null;
                    byte[] data3 = new byte[1];
                    input.read(data1);
                    int length = LocalModeManage.byteToInt(data1[2], data1[3]);// 长度转换成int
                    if (length > 4) {
                        data2 = new byte[length - 4];
                        input.read(data2);
                        input.read(data3);
                        if ((data1[4] & 0xFF) == MessageService.RECEIVE_HEARTBEAT_PACKAGE) {
                            message.what = MessageService.DEVICE_BACK_HEARTBEAT;
                        } else if ((data1[4] & 0xFF) == MessageService.RECEIVE_MESSAGE_PACKAGE) {
                            message.what = MessageService.DEVICE_BACK_PARAMETER;
                            message.obj = new String(data2);
                        } else if ((data1[4] & 0xFF) == MessageService.SET_PARAMETER_BACK) {
                            message.what = MessageService.DEVICE_BACK_SETPARAMETER;
                            message.obj = new String(data2);
                        } else {
                            message.what = MessageService.DEVICE_BACK_UNKONW;
                            message.obj = new String(data2);
                        }
                    } else {
                        data2 = new byte[4];
                        input.read(data2);
                        input.read(data3);
                    }
                }catch (SocketTimeoutException e){
                    e.printStackTrace();
                }catch (SocketException e){
                    e.printStackTrace();
                } catch (IOException e) {
                    message.what = MessageService.SOCKET_CONN_DISCONNECT;
                    e.printStackTrace();
                }
                handler.sendMessage(message);
            }
        }

        public void stopMessage() {
            try {
                this.run = false;
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
