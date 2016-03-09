package digimagus.csrmesh.acplug;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import digimagus.csrmesh.acplug.util.Utils;
import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.GroupDevice;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.EditTextWithDel;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * 新增组 修改组名、设备名称
 */
public class EditNameActivity extends BaseActivity implements View.OnClickListener {

    private EditTextWithDel edit_name;
    private ImageView back;
    private TextView done, title;

    private String editType = null;

    private String[] editTypes = new String[]{"addGroup", "editGroup", "editDevice"};
    private Bundle bundle;
    private int id;

    @Override
    protected void handler(Message message) {
        Log.e(TAG, "udp: " + message.obj);
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_editname);
        initFindViewById();
        bundle = getIntent().getBundleExtra("bundle");
        editType = bundle.getString("editType");
    }

    private void initFindViewById() {
        edit_name = (EditTextWithDel) findViewById(R.id.edit_name);
        edit_name.setImeOptions(EditorInfo.IME_ACTION_DONE);
        back = (ImageView) findViewById(R.id.back);
        done = (TextView) findViewById(R.id.done);
        title = (TextView) findViewById(R.id.title);
        back.setOnClickListener(this);
        done.setOnClickListener(this);
        edit_name.setOnEditorActionListener(onEditorActionListener);
    }

    TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            switch (actionId) {
                case EditorInfo.IME_ACTION_DONE:
                    Log.e(TAG, "IME_ACTION_DONE : " + v.getText());

                    String name = v.getText().toString().trim();
                    if (name.length() <= 0) {
                        tips(getString(R.string.not_empty)).setPositiveButton(R.string.yes, null).create().show();
                    } else if (Utils.sqlInjection(name)) {
                        tips(getString(R.string.contain_sql_keyword)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                    } else if (Utils.sqlInjection(name) || (name.length() < 2 || name.length() > 18)) {
                        if (editType.equals(editTypes[1]) || editType.equals(editTypes[0])) {
                            tips(getString(R.string.enter_group_message)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                        } else if (editType.equals(editTypes[2])) {
                            tips(getString(R.string.enter_device_message)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                        }
                    } else {
                        if (progressDialog == null) {
                            showProgressDialog(EditNameActivity.this);
                        }
                        if (editType.equals(editTypes[0])) {
                            for (GroupDevice group : mGroupDevices.values()) {
                                if (group.getName().equals(name)) {
                                    tips(getString(R.string.groupname_already_exists)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                                    return false;
                                }
                            }
                            GroupDevice info = new GroupDevice();
                            info.setName(name);
                            Map<String, DeviceInfo> map = new HashMap<>();
                            info.setDevices(map);
                            mDeviceStore.addGroupInfo(info);
                            progressDialog.setMessage(getString(R.string.add_group)).show();
                        } else if (editType.equals(editTypes[1])) {//
                            if (name.equals(bundle.getString("name"))) {
                                finish();
                                return false;
                            } else {
                                for (GroupDevice group : mGroupDevices.values()) {
                                    if (name.equals(group.getName())) {
                                        tips(getString(R.string.groupname_already_exists)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                                        return false;
                                    }
                                }
                            }
                            GroupDevice group = mDeviceStore.getGroupById(bundle.getInt("id"));
                            group.setName(name);
                            mDeviceStore.addGroupInfo(group);
                            progressDialog.setMessage(getString(R.string.update_group)).show();
                        } else if (editType.equals(editTypes[2])) {
                            DeviceInfo info = devices.get(bundle.getString("serial"));
                            devices.get(info.getSerial()).setName(name);
                            info.setId(bundle.getInt("id"));
                            info.setName(name);
                            Log.e(TAG, "ID: " + info.getId());
                            progressDialog.setMessage(getString(R.string.update_device)).show();
                            mDeviceStore.updateDevice(info.getSerial(), info);
                        }
                        handler.postDelayed(saveRunnable, 2000);
                    }
                    break;
            }
            return true;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.done:
                String name = edit_name.getText().toString().trim();
                if (name.length() <= 0) {
                    tips(getString(R.string.not_empty)).setPositiveButton(R.string.yes, null).create().show();
                } else if (Utils.sqlInjection(name)) {
                    tips(getString(R.string.contain_sql_keyword)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                } else if (Utils.sqlInjection(name) || (name.length() < 2 || name.length() > 18)) {
                    if (editType.equals(editTypes[1]) || editType.equals(editTypes[0])) {
                        tips(getString(R.string.enter_group_message)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                    } else if (editType.equals(editTypes[2])) {
                        tips(getString(R.string.enter_device_message)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                    }
                } else {
                    if (progressDialog == null) {
                        showProgressDialog(EditNameActivity.this);
                    }
                    if (editType.equals(editTypes[0])) {
                        for (GroupDevice group : mGroupDevices.values()) {
                            if (group.getName().equals(name)) {
                                tips(getString(R.string.groupname_already_exists)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                                return;
                            }
                        }
                        GroupDevice info = new GroupDevice();
                        info.setName(name);
                        Map<String, DeviceInfo> map = new HashMap<>();
                        info.setDevices(map);
                        mDeviceStore.addGroupInfo(info);
                        progressDialog.setMessage(getString(R.string.add_group)).show();
                    } else if (editType.equals(editTypes[1])) {//
                        if (name.equals(bundle.getString("name"))) {
                            finish();
                            return;
                        } else {
                            for (GroupDevice group : mGroupDevices.values()) {
                                if (name.equals(group.getName())) {
                                    tips(getString(R.string.groupname_already_exists)).setTitle(R.string.tips).setPositiveButton(R.string.yes, null).create().show();
                                    return;
                                }
                            }
                        }
                        GroupDevice group = mDeviceStore.getGroupById(bundle.getInt("id"));
                        group.setName(name);
                        mDeviceStore.addGroupInfo(group);
                        progressDialog.setMessage(getString(R.string.update_group)).show();
                    } else if (editType.equals(editTypes[2])) {
                        DeviceInfo info = devices.get(bundle.getString("serial"));
                        devices.get(info.getSerial()).setName(name);
                        info.setId(bundle.getInt("id"));
                        info.setName(name);
                        Log.e(TAG, "ID: " + info.getId());
                        progressDialog.setMessage(getString(R.string.update_device)).show();
                        mDeviceStore.updateDevice(info.getSerial(), info);
                    }
                    handler.postDelayed(saveRunnable, 2000);
                }
                break;
        }
    }

    Handler handler = new Handler();

    Runnable saveRunnable = new Runnable() {
        @Override
        public void run() {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            finish();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (editType.equals(editTypes[0])) {
            title.setText(getString(R.string.add_group));
            edit_name.setHint(getString(R.string.enter_group_name));
        } else if (editType.equals(editTypes[1])) {
            title.setText(getString(R.string.edit_group));
            edit_name.setHint(getString(R.string.enter_group_name));
            edit_name.setText(bundle.getString("name"));
        } else if (editType.equals(editTypes[2])) {
            title.setText(getString(R.string.edit_device));
            edit_name.setHint(getString(R.string.enter_device_name));
            edit_name.setText(bundle.getString("name") == null ? bundle.getString("serial") : bundle.getString("name"));
            id = bundle.getInt("id");
        }
    }

    private final static String TAG = "EditNameActivity";

    private ProgressBarDialog progressDialog;

    private ProgressBarDialog showProgressDialog(Context context) {
        progressDialog = new ProgressBarDialog(context).createDialog(context);
        progressDialog.setMessage(getString(R.string.save_data));
        return progressDialog;
    }

    private CustomDialog.Builder tips(String msg) {
        if (ibuilder == null) {
            ibuilder = new CustomDialog.Builder(this);
        }
        ibuilder.setTitle(R.string.tips);
        ibuilder.setMessage(msg);
        return ibuilder;
    }

    private CustomDialog.Builder ibuilder;
}
