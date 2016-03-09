package digimagus.csrmesh.acplug.listener;

/**
 * 设备状态监听 Listener
 */
public interface DeviceStatusListener {
    void deviceStatus(String serial,boolean status,boolean online);
}
