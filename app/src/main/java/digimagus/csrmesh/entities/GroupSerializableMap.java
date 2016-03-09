package digimagus.csrmesh.entities;

import java.io.Serializable;
import java.util.Map;

/**
 * 序列化map供Bundle传递map使用
 * Map 集合 组设备的Schedule
 */
public class GroupSerializableMap implements Serializable {

    private Map<String,Map<Integer,ScheduleInfo>> schedules;

    public Map<String, Map<Integer, ScheduleInfo>> getSchedules() {
        return schedules;
    }

    public void setSchedules(Map<String, Map<Integer, ScheduleInfo>> schedules) {
        this.schedules = schedules;
    }
}
