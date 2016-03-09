package digimagus.csrmesh.acplug.listener;

import digimagus.csrmesh.entities.DeviceInfo;

public interface DeviceMainFragmentListener {
    /**
     * 查询设备列表
     *
     * @param info
     */
    void queryDeviceList(DeviceInfo info);
    /**
     *              更新设备状态
     * @param serial 设备序列号
     * @param status   更新设备的状态
     * @param power    设备的功率
     * @param timeleft 倒计时时间
     */
    void updateDeviceStatus(String serial,boolean online,int status,double power,int timeleft);
    /**
     * 通知设备掉线在线状态
     * @param serial 设备序列号
     * @param online 设备的在线状态
     */
    void backUdpOnlineInquiry(String serial,boolean online);
    /**
     * 设备的状态查询完成
     */
    void querycarryout(boolean statue);

    /**
     * 刷新设备
     */
    void refresh();
}
