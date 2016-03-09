package com.digimagus.aclibrary;

import android.os.Handler;
import android.os.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 发送消息Socket
 */
public class SendMessageSocket {
    public Socket sendSocket;
    private String IP;
    private int PORT;
    private ReadThread readThread = null;
    private ExecutorService singleThreadExecutor = Executors.newFixedThreadPool(2);
    private long readMsgTime = 0;
    private String serial;
    private Handler sockethandler = new Handler();
    private Handler msgHandler;


    //本地模式设备掉线
    public final static int LOCAL_DROPPED=111;

    public SendMessageSocket(String ip, int port, String serial,Handler handler) {
        this.msgHandler=handler;
        this.IP = ip;
        this.PORT = port;
        this.serial = serial;
    }

    Runnable sendMsgRunnable = new Runnable() {
        @Override
        public void run() {
            if (sendSocket != null && sendSocket.isConnected()) {
                if (data != null) {
                    try {
                        OutputStream output = sendSocket.getOutputStream();
                        output.write(data);
                        output.flush();
                        readMsgTime = System.currentTimeMillis();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        data = null;
                    }
                }
            } else if(IP!=null&&PORT!=0&&msgHandler!=null){
                connSocket(IP, PORT, msgHandler);
                if (data != null) {
                    sendMesage(data);
                }
            }
        }
    };

    private byte[] data = null;

    public void sendMessage(byte[] data) {
        this.data = data;
        singleThreadExecutor.execute(sendMsgRunnable);
    }

    private int heartnum = 0;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - readMsgTime > 6 * 1000) {
                readMsgTime = System.currentTimeMillis();
                heartnum++;
                sendMessage(MessageService.READ_SOCKET_HEART);
                if (heartnum >= 2) {
                    sockethandler.removeCallbacks(runnable);
                    Message message=msgHandler.obtainMessage();
                    message.what=LOCAL_DROPPED;
                    message.obj=serial;
                    msgHandler.sendMessage(message);
                    IP=null;
                    PORT=0;
                }
            }
            sockethandler.postDelayed(this, 2* 1000);
        }
    };

    public void sendMesage(final byte[] data) {
        sendMessage(data);
    }


    private void connSocket(String ip, int port, Handler handler) {
        try {
            sendSocket = new Socket(ip, port);
            sendSocket.setKeepAlive(true);
            sendSocket.setSoTimeout(10 * 1000);
            readThread = new ReadThread(handler, sendSocket);
            readThread.start();
            readMsgTime = System.currentTimeMillis();
            singleThreadExecutor.execute(runnable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void closeSendMessage() {
        if(readThread!=null){
            readThread.closeT();
        }
        sockethandler.removeCallbacks(runnable);
    }

    class ReadThread extends Thread {
        private Handler handler;
        private boolean sendMsg = true;
        private Socket socket;

        public ReadThread(Handler handler, Socket readSocket) {
            this.handler = handler;
            this.socket = readSocket;
        }

        public void closeT() {
            try {
                sendMsg = false;
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            while (sendMsg) {
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
                        Message message = handler.obtainMessage();
                        if ((data1[4] & 0xFF) == MessageService.RECEIVE_HEARTBEAT_PACKAGE) {
                            heartnum = 0;
                           /* message.what = CONSTANT.DEVICE_BACK_HEARTBEAT;
                            message.obj = DEVSN;*/
                        } else if ((data1[4] & 0xFF) == MessageService.RECEIVE_MESSAGE_PACKAGE) {
                            byte[] heads = new byte[]{data1[2], data1[3], data1[4]};
                            //校验 数据是否正确
                            if (LocalModeManage.getCrc8(LocalModeManage.byteMerger(heads, data2, null), length - 1) == data3[0]) {
                                message.what = MessageService.DEVICE_BACK_PARAMETER;
                                String data = new String(data2).trim();
                                message.obj = data.replace("\"*\"", "\"" + serial + "\"");
                            }
                        } else if ((data1[4] & 0xFF) == MessageService.SET_PARAMETER_BACK) {
                            message.what = MessageService.DEVICE_BACK_SETPARAMETER;
                            message.obj = "{\"serial\":\""+serial+"\",\"result\":"+data2[0]+"}";
                        }
                        handler.sendMessage(message);
                        readMsgTime = System.currentTimeMillis();
                    }
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static final String TAG = "SendMessageSocket";
}