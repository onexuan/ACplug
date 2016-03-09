package digimagus.csrmesh.entities;

import java.util.Map;

/**
 * 组与设备关系表
 */
public class GroupDevice extends Information {

    public boolean status;
    public boolean online;
    public boolean activated;

    public GroupDevice(int id) {
        super(id);
    }

    public GroupDevice() {
    }

    private Map<String, DeviceInfo> devices;

    public Map<String, DeviceInfo> getDevices() {
        return devices;
    }

    public void setDevices(Map<String, DeviceInfo> devices) {
        this.devices = devices;
    }
}