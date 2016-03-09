package digimagus.csrmesh.acplug.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
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

import digimagus.csrmesh.acplug.EditNameActivity;
import digimagus.csrmesh.acplug.GroupSettingActivity;
import digimagus.csrmesh.acplug.MainActivity;
import digimagus.csrmesh.acplug.R;
import digimagus.csrmesh.acplug.listener.DeviceStatusListener;
import digimagus.csrmesh.acplug.listener.FragmentController;
import digimagus.csrmesh.adapter.SlipGMenuAdapter;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.GroupDevice;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;
import digimagus.csrmesh.view.SSwipeRefreshLayout;

/**
 * 组设备
 */
public class GroupMainFragment extends Fragment implements DeviceStatusListener, View.OnClickListener, SlipGMenuAdapter.ControlGroupListener {
    private final static String TAG = "GroupMainFragment";
    DisplayMetrics dm = new DisplayMetrics();
    Handler handler = new Handler();

    private View contextView;
    private ImageView add_group;
    private ListView group_list;
    private List<GroupDevice> mGroupInfos = new ArrayList<>();
    private SlipGMenuAdapter slipMenuAdapter = null;
    public Activity context;
    public SSwipeRefreshLayout refreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contextView = inflater.inflate(R.layout.fragment_groupmain, container, false);
        initFindViewById();
        return contextView;
    }

    @Override
    public void controlGroup(int position, View v) {
        final GroupDevice group = mGroupInfos.get(position);
        switch (v.getId()) {
            case R.id.right:
                if (mFragmentController.getMobileNetworkState() == MessageService.CONN_NETWORK_TYPE_NONE) {
                    tips(getString(R.string.tip), getString(R.string.connect_to_network)).setPositiveButton(R.string.yes, null).create().show();
                } else {
                    if (group.getDevices() != null && group.getDevices().size() > 0 && group.online) {
                        v.findViewById(R.id.send_msg).setVisibility(View.VISIBLE);
                        ((ImageView) v.findViewById(R.id.device_state)).setImageResource(R.mipmap.icon_activated);
                        mFragmentController.controlGroup(group.getId(), group.status);
                    } else if (!group.online && group.getDevices().size() == 0) {
                        tips(getString(R.string.tip), getString(R.string.add_device_group)).setPositiveButton(R.string.yes, null).create().show();
                    } else if (!group.online) {
                        tips(getString(R.string.tips), getString(R.string.group_offonline)).setPositiveButton(R.string.yes, null).create().show();
                    }
                }
                break;
            case R.id.setting:
                Bundle bundle = new Bundle();
                bundle.putInt("id", group.getId());
                bundle.putString("name", group.getName());
                mFragmentController.jumpActivity(GroupSettingActivity.class, bundle);
                break;
            case R.id.delete:
                tips(getString(R.string.delete_group_tips), getString(R.string.delete_group, group.getName())).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        handler.postDelayed(deleteRunnable, 2000);
                        mFragmentController.removeGroupById(group.getId());
                        mGroupInfos.remove(group);
                        mGroupInfos.remove(group);
                        showProgressDialog(getString(R.string.remove_group, group.getName())).show();
                    }
                }).setNegativeButton(R.string.no, null).create().show();
                break;
        }
    }

    private void initFindViewById() {
        add_group = (ImageView) contextView.findViewById(R.id.add_group);
        group_list = (ListView) contextView.findViewById(R.id.group_list);
        refreshLayout = (SSwipeRefreshLayout) contextView.findViewById(R.id.refreshLayout);
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        slipMenuAdapter = new SlipGMenuAdapter(context, mGroupInfos, dm.widthPixels);
        slipMenuAdapter.setControlGroupListener(this);
        group_list.setAdapter(slipMenuAdapter);
        add_group.setOnClickListener(this);
        refreshLayout.setOnRefreshListener(refreshListener);

    }

    SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            mFragmentController.getAllDeviceState();
        }
    };


    Runnable deleteRunnable = new Runnable() {
        @Override
        public void run() {
            slipMenuAdapter.notifyDataSetChanged(mGroupInfos);
            if (progress != null) {
                progress.dismiss();
            }
        }
    };


    @Override
    public void onStart() {
        super.onStart();
        mFragmentController.setStatusListener(this);
        mGroupInfos = mFragmentController.getAllGroups();
        for (GroupDevice group : mGroupInfos) {
            if (group.getDevices() == null || group.getDevices().isEmpty()) {
                group.online = false;
            } else {
                int index=0;
                for (DeviceInfo device : mFragmentController.getDevices(group.getId())) {
                    device = mFragmentController.getDevice(device.getSerial());
                    if (device != null) {
                        Log.e(TAG, "onStart  ->   Device State : " + device.online + "   " +group.status + "  " + device.state);
                        if (device.online) {
                            if ((device.state == 1&&group.status)||(!group.status&&index==0&&device.state==1)) {
                                group.status = true;
                            } else{
                                group.status = false;
                            }
                            group.online = true;
                            index++;
                        } else {
                            group.online = false;
                            break;
                        }
                    }
                }
            }
        }
        slipMenuAdapter.notifyDataSetChanged(mGroupInfos);
    }

    public FragmentController mFragmentController = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFragmentController = (MainActivity) activity;
        this.context = activity;
        Log.e(TAG, " --> onAttach ");
        mFragmentController.setStatusListener(this);
        //mFragmentController.getAllDeviceState();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_group:
                Bundle bundle = new Bundle();
                bundle.putString("editType", "addGroup");
                mFragmentController.jumpActivity(EditNameActivity.class, bundle);
                break;
        }
    }

    private CustomDialog.Builder ibuilder;

    private CustomDialog.Builder tips(String title, String msg) {
        ibuilder = new CustomDialog.Builder(context);
        ibuilder.setTitle(title);
        ibuilder.setMessage(msg);
        return ibuilder;
    }

    private ProgressBarDialog progress;

    private ProgressBarDialog showProgressDialog(String msg) {
        progress = new ProgressBarDialog(context).createDialog(context);
        progress.setMessage(msg);
        return progress;
    }

    @Override
    public void deviceStatus(String serial, boolean status, boolean online1) {
        //Log.e(TAG,"serial:"+serial+"     status:" + status+"   online:"+online1);
        refreshLayout.setRefreshing(false);
        for (GroupDevice group : mGroupInfos) {
            for (DeviceInfo device : mFragmentController.getDevices(group.getId())) {
                device = mFragmentController.getDevice(device.getSerial());
                if (device != null && device.getSerial().equals(serial)) {
                    device.state = status ? 1 : 0;
                    device.online = online1;
                }
            }
        }
        for (GroupDevice group : mGroupInfos) {
            if (group.getDevices() == null || group.getDevices().isEmpty()) {
                group.online = false;
            } else {
                int index=0;
                for (DeviceInfo device : mFragmentController.getDevices(group.getId())) {
                    device = mFragmentController.getDevice(device.getSerial());
                    if (device != null) {
                        if (device.online) {
                            if ((device.state == 1&&group.status)||(index==0&&device.state==1)) {
                                group.status = true;
                            } else{
                                group.status = false;
                            }
                            group.online = true;
                            index++;
                        } else {
                            group.online = false;
                            break;
                        }
                    }
                }
            }
        }
        slipMenuAdapter.notifyDataSetChanged(mGroupInfos);
    }
}
