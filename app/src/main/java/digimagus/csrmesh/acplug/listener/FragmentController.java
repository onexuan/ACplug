package digimagus.csrmesh.acplug.listener;

import android.os.Bundle;
import android.view.View;

import java.util.List;
import java.util.Map;

import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.GroupDevice;
import digimagus.csrmesh.entities.HideDeviceInfo;
import digimagus.csrmesh.entities.PhoneInfo;
import digimagus.csrmesh.entities.ScheduleInfo;
import digimagus.csrmesh.entities.Setting;

/**
 * FragmentController 控制接口
 */
public interface FragmentController {

    Map<String, DeviceInfo> getAllDevice();

    void removeGroupById(int groupId);

    /**
     * 读取配置信息
     */
    Setting getSetting();

    void getAllDeviceState();

    /**
     * 设置DeviceMainFragment的监听
     *
     * @param fragmentListener
     */
    void setDeviceListListener(DeviceMainFragmentListener fragmentListener);
    /**
     * @param activity 跳转 Activity
     * @param bundle   携带数据
     */
    void jumpActivity(Class activity, Bundle bundle);
    /**
     * 通知查询设备的状态
     *
     * @param
     */
    void queryAllDeviceState();
    /**
     * 通过 Group Id 来控制设备
     *
     * @param id
     */
    void controlGroup(int id, boolean status);

    /**
     * 设置设备的状态监听
     */
    void setStatusListener(DeviceStatusListener statusListener);
    /**
     * 查询APP的版本号
     */
    String appVersion();

    /**
     * 请求组中的设备
     *
     * @param groupId 组的Id
     * @return 返回组中的设备
     */
    List<DeviceInfo> getDevices(Integer groupId);
    /**
     * 得到设备的firmware
     *
     * @param serial 设备序列号
     * @return
     */
    DeviceInfo getDevice(String serial);
    /**
     * 移除设备
     * 移除数据库中此设备
     *
     * @param info
     */
    void removeDevice(DeviceInfo info);
    /**
     * 得到手机的当前网络状态
     * <p/>
     * WiFi
     * 手机移动网络
     * 无网络连接
     */
    int getMobileNetworkState();
    /**
     * 设置设备的状态
     *
     * @param info
     */
    void setDeviceState(DeviceInfo info,View v);

    List<GroupDevice> getAllGroups();
}
