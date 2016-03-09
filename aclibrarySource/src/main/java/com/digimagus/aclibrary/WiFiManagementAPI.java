package com.digimagus.aclibrary;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

/**
 * WiFi管理工具类
 * 扫描WiFi
 * 连接WiFi
 */
public class WiFiManagementAPI {
    private final static String TAG = "WiFiManagementAPI";
    private static WiFiManagementAPI instance = null;

    public final static int PING_IP_SUCCESS = 600;
    public final static int PING_IP_FAILURE = 601;
    public final static int PING_IP_COMPLETE = 602;

    private static Context mContext;

    /**
     * 单例模式
     *
     * @return 返回WiFi管理类的对象
     */
    private static class LazyHolder {
        private static final WiFiManagementAPI INSTANCE = new WiFiManagementAPI(mContext);
    }

    public static final WiFiManagementAPI getInstance(Context context) {
        mContext = context;
        return LazyHolder.INSTANCE;
    }

    /**
     * WiFi加密类型的枚举定义
     * <p/>
     * 该枚举定义了一般WiFi的加密方式
     */
    public enum WiFiEncryptionType {
        /**
         * WiFi WPA 加密方式
         */
        WiFi_TYPE_WPA,
        /**
         * WiFi WEP 加密方式
         */
        WiFi_TYPE_WEP,
        /**
         * WiFi ESS 加密方式
         */
        WiFi_TYPE_ESS,
        /**
         * WiFi UNKNOWN 加密方式
         */
        WiFi_TYPE_UNKNOWN
    }

    /**
     * 管理WiFi各方面的连接
     */
    private WifiManager mWifiManager;
    /**
     * 描述的任何Wifi连接处于活动状态或状态
     */
    private WifiInfo mWifiInfo;
    /***
     * 扫描到的WiFi列表
     */
    private List<ScanResult> mWifiList = null;

    /***
     * 初始化 WIFI相关处理类
     *
     * @param context 上下文对象
     */
    public WiFiManagementAPI(Context context) {
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiInfo = mWifiManager.getConnectionInfo();
    }

    /**
     * 检测 是否可以连接到外网
     *
     * @param URL ping 的IP 地址 或者 是 网址
     * @return 返回网络是否可以连接外网到外网使用  true可以   false不能
     */
    public boolean ping(String URL) {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 10 " + URL);
            int status = p.waitFor();
            if (status == 0) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * 得到手机的MAC地址
     * 得到手机的网卡MAC
     * 注册绑定设备、控制设备的标识
     *
     * @param context 上下文对象
     * @return 网卡的MAC 地址
     */
    public String getLocalMacAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getMacAddress();
    }

    /**
     * 打开WiFi
     * 若手机没有启用WiFi,打开WiFi
     *
     * @return 返回手机WiFi的启用状态
     */
    public boolean isopenWiFiNetWork() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return mWifiManager.isWifiEnabled();
    }

    /**
     * 关闭WiFi
     * 关闭WiFi
     */
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    /**
     * 请求当前WiFi的连接信息
     * 根据WiFi当前的连接信息 判断WiFi的加密方式等待
     *
     * @return 返回当前连接的WiFi配置信息
     */
    public WifiInfo getWiFiInfo() {
        return mWifiManager.getConnectionInfo();
    }

    /**
     * 请求当前手机配置过的WiFi信息
     *
     * @return 返回手机已配置过的WiFi信息集合
     */
    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfiguration;
    }

    /**
     * 扫描手机周围的WiFi列表
     * 扫描手机周围的WiFi
     *
     * @return 返回手机扫描到周围设备的列表
     */
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }


    /**
     * 开始扫描WiFi
     * 启动开始扫描手机周围的WiFi
     */
    public void startScanWifi() {
        boolean scan = mWifiManager.startScan();
        mWifiList = mWifiManager.getScanResults();
        mWifiConfiguration = mWifiManager.getConfiguredNetworks();
    }

    /**
     * 添加一个WiFi配置信息
     * 添加WiFi配置信息到手机、用于切换手机的WiFi连接
     *
     * @param wcg WiFi配置相关信息
     * @return 配置是否添加成功
     */
    public boolean addWiFiNetwork(WifiConfiguration wcg) {
        int wcgID = mWifiManager.addNetwork(wcg);
        return mWifiManager.enableNetwork(wcgID, true);
    }

    /**
     * 创建并设置WiFi信息
     *
     * @param SSID     连接WiFi的名称
     * @param Password 连接WiFi的密码
     * @param Type     WiFi的加密类型
     * @return 返回WiFi的配置信息
     */
    public WifiConfiguration createWifiInfo(String SSID, String Password, WiFiEncryptionType Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        WifiConfiguration tempConfig = IsExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }
        if (Type == WiFiEncryptionType.WiFi_TYPE_WPA) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } else if (Type == WiFiEncryptionType.WiFi_TYPE_WEP) {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (Type == WiFiEncryptionType.WiFi_TYPE_ESS) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (Type == WiFiEncryptionType.WiFi_TYPE_UNKNOWN) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        return config;
    }

    /**
     * 查看WiFi配置信息是否存在
     *
     * @param SSID WiFi的名称
     * @return 返回WiFi的配置信息
     */
    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    /**
     * WiFi配置集合
     */
    private List<WifiConfiguration> mWifiConfiguration;

    /**
     * WiFi加密类型数组
     */
    public final static String[] WIFI_ENCRYPTION_TYPE = new String[]{
            "wpa", "wep", "ess", ""
    };

    /**
     * 验证WiFi的加密类型
     * 根据WiFi扫描出的capabilities 值 检验WiFi的加密类型 , 便于手机能够连接WiFi
     *
     * @param type ScanResults  capabilities
     * @return 返回WiFi的枚举类型
     */
    public WiFiManagementAPI.WiFiEncryptionType verifyWiFiType(String type) {
        String vertype = "";
        if (type != null) {
            for (String str : WIFI_ENCRYPTION_TYPE) {
                if (type.toLowerCase().indexOf(str.toLowerCase()) != -1) {
                    vertype = str;
                    break;
                }
            }
        }
        if (vertype.equals(WIFI_ENCRYPTION_TYPE[0])) {
            return WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_WPA;
        } else if (vertype.equals(WIFI_ENCRYPTION_TYPE[1])) {
            return WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_WEP;
        } else {
            return WiFiManagementAPI.WiFiEncryptionType.WiFi_TYPE_ESS;
        }
    }
}