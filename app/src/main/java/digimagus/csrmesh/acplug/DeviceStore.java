package digimagus.csrmesh.acplug;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import digimagus.csrmesh.database.DataBaseDataSource;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.GroupDevice;
import digimagus.csrmesh.entities.HideDeviceInfo;
import digimagus.csrmesh.entities.PhoneInfo;
import digimagus.csrmesh.entities.ScheduleInfo;
import digimagus.csrmesh.entities.Setting;

/**
 *
 */
public class DeviceStore {
    private final static String TAG = "DeviceStore";
    Map<String, DeviceInfo> mDevices = new LinkedHashMap<>();
    private Map<String, HideDeviceInfo> mHideDevices = new LinkedHashMap<>();
    private Map<Integer, GroupDevice> mGroups_ = new LinkedHashMap<>();
    private Map<Integer, List<ScheduleInfo>> groupSchedule = new LinkedHashMap<>();

    private DataBaseDataSource mDataBase;
    private Setting mSetting;
    public static PhoneInfo phoneInfo;

    public PhoneInfo getPhoneInfo() {
        return phoneInfo;
    }

    static Context mContext;

    private static class LazyHolder {
        private static final DeviceStore INSTANCE = new DeviceStore(mContext);
    }

    public static final DeviceStore getInstance(Context context) {
        mContext = context;
        return LazyHolder.INSTANCE;
    }

    public DeviceStore(Context context) {
        mDataBase = new DataBaseDataSource(context);
    }

    public boolean loadAllInfo() {
        // clear devices and groups lists.
        clearDevices();
        this.phoneInfo = mDataBase.queryPhoneInfo();
        this.mSetting = mDataBase.getSetting();
        // get SingleDevices and GroupDevices from database.
        List<DeviceInfo> devices = mDataBase.getAllDevices();
        for (DeviceInfo dev : devices) {
            this.mDevices.put(dev.getSerial(), dev);
        }
        List<GroupDevice> groups = mDataBase.getAllGroups();
        for (GroupDevice grou : groups) {
            if (grou.getDevices() != null && grou.getDevices().size() != 0) {
                Map<String, DeviceInfo> device = new HashMap<>();
                for (DeviceInfo info : grou.getDevices().values()) {
                    device.put(info.getSerial(), mDevices.get(info.getSerial()));
                }
                grou.setDevices(device);
            }
            mGroups_.put(grou.getId(), grou);
        }

        List<HideDeviceInfo> hideDevices = mDataBase.getHideDevices();
        for (HideDeviceInfo hides : hideDevices) {
            mHideDevices.put(hides.getSerial(), hides);
        }

        for (GroupDevice group : groups) {
            List<ScheduleInfo> scheduleInfos = mDataBase.getGroupSchedule(group.getId());
            groupSchedule.put(group.getId(), scheduleInfos);
        }
        return devices.size() > 0 ? true : false;
    }

    public void updateSettings(Setting setting) {
        mDataBase.createSetting(setting);
    }

    public PhoneInfo updatePhoneInfo(PhoneInfo info) {
        phoneInfo = mDataBase.updatePhoneInfo(info);
        return phoneInfo;
    }

    /**
     * 查询数据库设备
     *
     * @return
     */
    public Map<String, DeviceInfo> getAllDevices() {
        Map<String, DeviceInfo> devices = new LinkedHashMap<>();
        List<DeviceInfo> deviceInfos = mDataBase.getAllDevices();
        for (DeviceInfo d : deviceInfos) {
            devices.put(d.getSerial(), d);
        }
        return devices;
    }

    /**
     * 查询数据库隐藏的设备
     *
     * @return
     */
    public Map<String, HideDeviceInfo> getHideDevices() {
        return mHideDevices;
    }

    /**
     * 查询组中的设备
     *
     * @return
     */
    public Map<Integer, GroupDevice> getAllGroups() {
        return mGroups_;
    }

    /**
     * 查找组中的设备
     *
     * @param groupId 根据组的Id
     * @return
     */
    public List<DeviceInfo> getGroupDeviceById(int groupId) {
        List<DeviceInfo> infos = new ArrayList<>();
        for (DeviceInfo d : mGroups_.get(groupId).getDevices().values()) {
            infos.add(d);
        }
        return infos;
    }

    /**
     * 移除一个组
     */
    public void removeGroupById(int groupId) {
        mGroups_.remove(groupId);
        mDataBase.removeGroupById(groupId);
    }

    /**
     * 移除一个设备
     * 并且移除组中的设备
     */
    public void removeDeviceById(int deviceId, String devsn) {
        mDevices.remove(devsn);
        mDataBase.removeDeviceById(deviceId);
        //删除缓存数据
        for (GroupDevice group : mGroups_.values()) {
            group.getDevices().remove(devsn);
        }
        //删除数据库
        mDataBase.deleteGroupDevice(devsn);
    }

    /**
     * 移除组中的设备
     *
     * @param groupId 组的Id 号
     * @param devsn   设备的序列号
     */
    public void removeDeviceGroup(int groupId, String devsn) {
        mDataBase.deleteGroupDevice(groupId, devsn);
        mGroups_.get(groupId).getDevices().remove(devsn);
    }

    /**
     * 根据 deviceId 查询一个设备信息
     *
     * @param devsn 序列号
     * @return
     */
    public DeviceInfo getDeviceInfo(String devsn) {
        return mDevices.get(devsn);
    }

    public GroupDevice getGroupById(Integer id) {
        return mGroups_.get(id);
    }

    /**
     * 更新一个设备信息 或者添加 一个设备
     *
     * @param deviceInfo
     */
    public DeviceInfo addDeviceInfo(DeviceInfo deviceInfo) {
        for (DeviceInfo d : mDevices.values()) {
            Log.e(TAG,"serial: "+d.getSerial());
        }

        if (mDevices.get(deviceInfo.getSerial()) == null) {
            deviceInfo = mDataBase.createDevice("add", deviceInfo);
        } else {
            deviceInfo = mDataBase.createDevice("update", deviceInfo);
        }

        if (deviceInfo != null && deviceInfo.getSerial() != null) {
            mDevices.put(deviceInfo.getSerial(), deviceInfo);
        }
        //判断隐藏表中是否有数据
        if (deviceInfo != null && deviceInfo.getSerial() != null && mHideDevices.get(deviceInfo.getSerial()) != null) {
            removeHideDevice(mHideDevices.get(deviceInfo.getSerial()));
        }
        return deviceInfo;
    }

    public GroupDevice addGroupInfo(GroupDevice groupInfo) {
        groupInfo = mDataBase.createGroup(groupInfo);
        if (groupInfo != null) {
            mGroups_.put(groupInfo.getId(), groupInfo);
        }
        return groupInfo;
    }

    public void updateGroupById(Integer id, String s) {
        boolean isexit = false;
        for (DeviceInfo device : mGroups_.get(id).getDevices().values()) {
            if (s.equals(device.getSerial())) {
                isexit = true;
                break;
            }
        }
        if (!isexit) {
            Map<String, DeviceInfo> devices = mGroups_.get(id).getDevices();
            devices.put(s, mDevices.get(s));
            mDataBase.updateGroupDevice(id, s);
            mGroups_.get(id).setDevices(devices);
        }
    }

    public List<ScheduleInfo> getGroupSchedulesById(Integer groupId) {
        return groupSchedule.get(groupId);
    }

    public void addGroupSchedule(ScheduleInfo info, Integer groupId) {
        List<ScheduleInfo> scheduleInfos = groupSchedule.get(groupId);
        if (scheduleInfos == null) {
            scheduleInfos = new ArrayList<>();
        }
        ScheduleInfo scheduleInfo=mDataBase.createGroupSchedule(info, groupId);

        Log.e(TAG, "addGroupSchedule----->  ID:" +scheduleInfo.id);
        scheduleInfos.add(scheduleInfo);
        groupSchedule.put(groupId, scheduleInfos);
    }

    /**
     * 清除数据
     *
     * @return
     */
    public boolean deleteDataBase() {

        return false;
    }

    /**
     * Clear the single and group devices lists.
     */
    private void clearDevices() {
        mDevices.clear();
        mHideDevices.clear();
        mGroups_.clear();
        mSetting = null;
    }

    public Setting getSetting() {
        return mSetting;
    }




    /**
     * 查询所有的单个设备
     *
     * @return
     */
    public Map<String, DeviceInfo> getDevices() {
        return mDevices;
    }

    /**
     * 根据设备的序列号，更新设备的信息
     *
     * @param serial 设备序列号
     * @param info   设备信息
     * @return
     */
    public boolean updateDevice(String serial, DeviceInfo info) {
        try {
            mDevices.put(serial,info);
            mDataBase.createDevice("update", info);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 添加隐藏设备的ID
     *
     * @param hide
     */
    public void addHideDevice(HideDeviceInfo hide) {
        HideDeviceInfo hideInfo = mHideDevices.get(hide.getSerial());
        if (hideInfo == null) {
            mDataBase.addHideDevice(hide);
        } else {
            hide.setId(hideInfo.getId());
        }
        mHideDevices.put(hide.getSerial(), hide);
    }

    public void removeHideDevice(HideDeviceInfo hide) {
        mHideDevices.remove(hide.getSerial());
        mDataBase.removeHideDevice(hide.getSerial());
    }

    public void removeGroupSchedule(int id) {
        mDataBase.removeGroupScheduleById(id);
    }
}
