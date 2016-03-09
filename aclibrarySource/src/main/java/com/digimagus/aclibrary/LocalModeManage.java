package com.digimagus.aclibrary;

import android.os.Handler;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多个 Socket 管理通信API
 * 本地模式管理 API
 */
public class LocalModeManage {
    /**
     * 单例模式
     *
     * @return 本地模式管理对象
     */
    private static class LazyHolder {
        private static final LocalModeManage INSTANCE = new LocalModeManage();
    }

    public static final LocalModeManage getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * 使用集合保存Socket连接
     */
    private Map<String, SendMessageSocket> sendSockets = new LinkedHashMap<>();

    /**
     * @param ip      设备的IP
     * @param port    设备的端口号
     * @param serial   设备序列号
     * @param data    发送的数据
     * @param status  0、1、
     * @param handler Handler
     */
    public void sendMessage(String ip, int port, String serial, String data, byte status, Handler handler) {
        SendMessageSocket sendSocket = sendSockets.get(serial);
        if (sendSocket == null) {
            sendSocket = new SendMessageSocket(ip, port, serial,handler);
        }
        sendSocket.sendMesage(getArray(data, status).array());
        sendSockets.put(serial, sendSocket);
    }

    public void removeDevice(String serial) {
        if (sendSockets.get(serial) != null) {
            sendSockets.get(serial).closeSendMessage();
            sendSockets.remove(serial);
        }
    }

    public void stopSendMessage() {
        for (SendMessageSocket socket : sendSockets.values()) {
            socket.closeSendMessage();
        }
        sendSockets.clear();
    }

    /**
     * 组装byte[] 数组
     *
     * @param sb     发送的json数据
     * @param status
     * @return 返回 ByteBuffer
     */
    private ByteBuffer getArray(String sb, byte status) {
        byte[] data = sb.getBytes();
        byte[] command = {status};
        byte[] packet = {(byte) (data.length + 4 & 255), (byte) (data.length + 4 >> 8)};
        byte[] byteAll = byteMerger(packet, command, data);
        byte[] crc = new byte[]{getCrc8(byteAll, packet.length + 1 + data.length)};
        ByteBuffer buffer = ByteBuffer.allocate(2 + packet.length + 1 + data.length + crc.length);
        buffer.put(MessageService.HEADER);
        buffer.put(packet);
        buffer.put(command);
        buffer.put(data);
        buffer.put(crc);
        buffer.flip();
        return buffer;
    }


    /**
     * 合并byte数组
     *
     * @param byte1 len数组
     * @param byte2 cmd数组
     * @param byte3 json数组
     * @return byteAll 组合后的数组
     */
    public static byte[] byteMerger(byte[] byte1, byte[] byte2, byte[] byte3) {
        // byte1与byte2合并
        byte[] byte4 = new byte[byte1.length + byte2.length];
        System.arraycopy(byte1, 0, byte4, 0, byte1.length);
        System.arraycopy(byte2, 0, byte4, byte1.length, byte2.length);
        if (byte3 != null) {
            // byte3与byte1,byte2合并后的数组再进行合并
            byte[] byteAll = new byte[byte4.length + byte3.length];
            System.arraycopy(byte4, 0, byteAll, 0, byte4.length);
            System.arraycopy(byte3, 0, byteAll, byte4.length, byte3.length);
            return byteAll;
        } else {
            return byte4;
        }
    }

    /**
     * CRC 累加校验
     *
     * @param ptr 所有的byte数据的数组
     * @param len 数组的长度
     * @return 返回累加校验byte
     */
    public static byte getCrc8(byte[] ptr, int len) {
        byte crc8 = 0;
        int i = 0;
        while (len-- != 0) {
            crc8 = (byte) ((ptr[i++] + crc8) % 256);
        }
        return crc8;
    }

    /**
     * 计算数据的长度
     *
     * @param b1 高八位
     * @param b2 低八位
     * @return 返回数据的长度
     */
    public static int byteToInt(byte b1, byte b2) {
        return ((b2 << 8) | (b1 & 0xFF));
    }

    private final static String TAG = "LocalModeManage";
}
