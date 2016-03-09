package digimagus.csrmesh.entities;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 序列化map供Bundle传递map使用
 * Map 集合 组设备的Schedule
 */
public class DeviceSerializableMap implements Serializable {

    private Map<Integer,ScheduleInfo> schedules;

    public Map<Integer,ScheduleInfo> getSchedules() {
        return schedules;
    }

    public void setSchedules(Map<Integer,ScheduleInfo> schedules) {
        this.schedules = schedules;
    }
}
