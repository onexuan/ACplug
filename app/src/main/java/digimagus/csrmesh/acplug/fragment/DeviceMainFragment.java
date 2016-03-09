package digimagus.csrmesh.acplug.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import com.digimagus.aclibrary.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import digimagus.csrmesh.acplug.DeviceSettingsActivity;
import digimagus.csrmesh.acplug.R;
import digimagus.csrmesh.acplug.SummaryActivity;
import digimagus.csrmesh.acplug.listener.DeviceMainFragmentListener;
import digimagus.csrmesh.acplug.listener.FragmentController;
import digimagus.csrmesh.adapter.SlipDMenuAdapter;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.SSwipeRefreshLayout;

/**
 * 单个设备
 */
public class DeviceMainFragment extends Fragment implements SlipDMenuAdapter.ControlDeviceListener, DeviceMainFragmentListener {
    private final static String TAG = "DeviceMainFragment";
    private View contextView;
    private ListView device_list;

    private SlipDMenuAdapter mDeviceAdapter;
    private SSwipeRefreshLayout refreshLayout;

    private Map<String, DeviceInfo> map = new ConcurrentHashMap<>();
    private List<DeviceInfo> mDeviceInfos = new ArrayList<>();
    public Activity context;
    private FragmentController mFragmentController;
    DisplayMetrics dm = new DisplayMetrics();

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contextView = inflater.inflate(R.layout.fragment_devicemain, container, false);
        mFragmentController.setDeviceListListener(this);
        initFindViewById();
        return contextView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        mFragmentController = (FragmentController) activity;
    }


    @Override
    public void refresh() {
        mFragmentController.setDeviceListListener(this);
        mDeviceInfos.clear();
        map = mFragmentController.getAllDevice();
        for (DeviceInfo d : map.values()) {
            mDeviceInfos.add(d);
        }
        mDeviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void controlDevice(final DeviceInfo info, View v) {
        switch (v.getId()) {
            case R.id.right:
                if (mFragmentController.getMobileNetworkState() == MessageService.CONN_NETWORK_TYPE_NONE) {
                    Tips(R.string.tips, getString(R.string.connect_to_network)).setPositiveButton(R.string.yes, null).create().show();
                } else {
                    if (info.online) {
                        mFragmentController.setDeviceState(info,v);
                    } else {//Device is not online
                        Tips(R.string.tips, getString(R.string.device_not_online)).setNegativeButton(R.string.yes, null).create().show();
                    }
                }
                break;
            case R.id.setting:
                Bundle bundle = new Bundle();
                bundle.putInt("id", info.getId());
                bundle.putString("name", info.getName());
                bundle.putString("serial", info.getSerial());
                bundle.putString("uuid", info.getUuid());
                mFragmentController.jumpActivity(DeviceSettingsActivity.class, bundle);
                break;
            case R.id.delete:
                Tips(R.string.remove_device_tips, getString(R.string.remove_device_msg, info.getName())).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mFragmentController.removeDevice(info);
                    }
                }).setNegativeButton(R.string.no, null).create().show();
                break;
            case R.id.content: {
                Bundle bundle_S = new Bundle();
                bundle_S.putInt("id", info.getId());
                bundle_S.putString("serial", info.getSerial());
                bundle_S.putString("online", info.online ? (info.state == 1 ? "ON" : "OFF") : "Offline");
                bundle_S.putString("name", info.getName());
                bundle_S.putString("uuid", info.getUuid());
                bundle_S.putDouble("power", info.power);
                Log.e(TAG, "UUID : " + info.getUuid());
                mFragmentController.jumpActivity(SummaryActivity.class, bundle_S);
                break;
            }
        }
    }

    private CustomDialog.Builder ibuilder = null;

    public CustomDialog.Builder Tips(int titleId, String msgId) {
        ibuilder = new CustomDialog.Builder(context);
        ibuilder.setTitle(titleId);
        ibuilder.setMessage(msgId);
        return ibuilder;
    }

    @Override
    public void queryDeviceList(DeviceInfo info) {
        if (map.get(info.getSerial()) == null) {
            map.put(info.getSerial(), info);
            mDeviceInfos.add(info);
        } else {
            if (map.get(info.getSerial()).getIP() == null || map.get(info.getSerial()).getPORT() == 0) {
                boolean exit = false;
                DeviceInfo deviceInfo = map.get(info.getSerial());
                deviceInfo.setIP(info.getIP());
                deviceInfo.setPORT(info.getPORT());
                map.put(deviceInfo.getSerial(), deviceInfo);
                for (DeviceInfo d : mDeviceInfos) {
                    if (info.getSerial().equals(d.getSerial())) {
                        d.setIP(info.getIP());
                        d.setPORT(info.getPORT());
                        exit = true;
                        break;
                    }
                }
                if (!exit) {
                    mDeviceInfos.add(deviceInfo);
                }
            } else {
                boolean exit = false;
                DeviceInfo deviceInfo = map.get(info.getSerial());
                deviceInfo.setIP(info.getIP());
                deviceInfo.setPORT(info.getPORT());
                map.put(deviceInfo.getSerial(), deviceInfo);
                for (DeviceInfo d : mDeviceInfos) {
                    if (info.getSerial().equals(d.getSerial())) {
                        d.setIP(info.getIP());
                        d.setPORT(info.getPORT());
                        exit = true;
                        break;
                    }
                }
                if (!exit) {
                    mDeviceInfos.add(deviceInfo);
                }
            }
        }
        mDeviceAdapter.notifyDataSetChanged();
    }


    private void initFindViewById() {
        device_list = (ListView) contextView.findViewById(R.id.device_list);
        refreshLayout = (SSwipeRefreshLayout) contextView.findViewById(R.id.refreshLayout);
        refreshLayout.setViewGroup(device_list);
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        mDeviceAdapter = new SlipDMenuAdapter(context, mDeviceInfos, dm.widthPixels);
        mDeviceAdapter.setControlDeviceListener(this);
        device_list.setAdapter(mDeviceAdapter);
        refreshLayout.setOnRefreshListener(refreshListener);
        refresh();
    }

    SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (mFragmentController.getMobileNetworkState() == MessageService.CONN_NETWORK_TYPE_NONE) {
                refreshLayout.setRefreshing(false);
                Tips(R.string.tips, getString(R.string.connect_to_network)).setPositiveButton(R.string.yes, null).create().show();
            } else if (mDeviceInfos.size() > 0) {
                mFragmentController.queryAllDeviceState();
            }
            handler.postDelayed(timeoutRunnable, 10 * 1000);
            //查询所有设备的状态
        }
    };

    Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Message message = handler.obtainMessage();
            message.what = 0x002;
            handler.sendMessage(message);
        }
    };

    @Override
    public void querycarryout(boolean statue) {
        Message message = handler.obtainMessage();
        message.what = 0x002;
        handler.sendMessage(message);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x001:
                    mDeviceAdapter.notifyDataSetChanged();
                    break;
                case 0x002:
                    refreshLayout.setRefreshing(false);
                    break;
            }
        }
    };

    private void updateUI() {
        Message message = handler.obtainMessage();
        message.what = 0x001;
        handler.sendMessage(message);
    }

    @Override
    public void backUdpOnlineInquiry(String serial, boolean online) {
        for (DeviceInfo device : mDeviceInfos) {
            if (serial.equals(device.getSerial())) {
                device.online = online;
                device.state = 0;
                device.setPower(0);
                device.remaining = 0;
                map.put(serial, device);
                updateUI();
                break;
            }
        }
    }

    /**
     * 更新设备状态
     *
     * @param serial
     * @param status
     */
    @Override
    public void updateDeviceStatus(String serial, boolean online, int status, double power, int timeleft) {
        if (status == -1 || serial == null) {
            updateUI();
        } else {
            for (DeviceInfo device : mDeviceInfos) {
                if (device != null && (serial.equals(device.getSerial()) || serial.equals(device.getUuid()))) {
                    device.online = online;
                    device.state = status;
                    device.setPower(power);
                    device.remaining = timeleft;
                    Log.e(TAG, "更新设备状态:  " + serial + "    " + online);
                    mDeviceAdapter.notifyDataSetChanged();
                    map.put(serial, device);
                    break;
                }
            }
        }
    }
}