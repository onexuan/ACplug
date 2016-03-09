package digimagus.csrmesh.acplug;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.digimagus.aclibrary.HTTPManagementAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.PhoneInfo;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * 导出
 */
public class ExportActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "ExportActivity";
    private ImageView back;
    private EditText email;
    private TextView send_email;
    private Pattern pattern = null;

    public ProgressBarDialog mProgressDialog;
    public HTTPManagementAPI mHttpManagementAPI;

    public PhoneInfo mPhoneInfo;
    public DeviceInfo mDeviceInfo;
    public int signal;
    public String serial;

    @Override
    protected void handler(Message msg) {
    }

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_export);
        mHttpManagementAPI = HTTPManagementAPI.getInstance();
        mProgressDialog = new ProgressBarDialog(this).createDialog(this);
        mProgressDialog.setMessage(getString(R.string.send));
        mPhoneInfo = (PhoneInfo) getIntent().getSerializableExtra("mPhoneInfo");
        serial = getIntent().getStringExtra("serial");
        mDeviceInfo = devices.get(serial);
        signal = getIntent().getIntExtra("signal", 0);
        initFindViewById();
        pattern = Pattern.compile("^[a-z0-9]+([._\\\\-]*[a-z0-9])*@([a-z0-9]+[-a-z0-9]*[a-z0-9]+.){1,63}[a-z0-9]+$");
    }


    private void initFindViewById() {
        back = (ImageView) findViewById(R.id.back);
        email = (EditText) findViewById(R.id.email);
        send_email = (TextView) findViewById(R.id.send_email);
        send_email.setOnClickListener(this);
        back.setOnClickListener(this);
        email.setImeOptions(EditorInfo.IME_ACTION_SEND);
        email.setOnEditorActionListener(onEditorActionListener);
    }

    TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (mDeviceInfo.getPrice() <= 0) {
                Tips(getString(R.string.tips), getString(R.string.export_device)).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        Intent intent = new Intent(ExportActivity.this, CostActivity.class);
                        intent.putExtra("serial", mDeviceInfo.getSerial());
                        startActivity(intent);
                    }
                }).setNegativeButton(R.string.no, null).create().show();
                return true;
            }
            final String email_str = v.getText().toString().trim();
            if (email_str.length() > 0) {
                Matcher matcher = pattern.matcher(email_str);
                boolean rs = matcher.matches();
                if (rs) {
                    mProgressDialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Map<String, String> head = new HashMap<>();
                                head.put("wislink_auth_uuid", mPhoneInfo.getUuid());
                                head.put("wislink_auth_token", mPhoneInfo.getToken());
                                Map<String, String> map = new HashMap<>();
                                map.put("devuuid", mDeviceInfo.getUuid());
                                map.put("devname", mDeviceInfo.getName());
                                map.put("emailaddr", email_str);
                                map.put("devmac", mDeviceInfo.getMac());
                                map.put("devstrength", String.valueOf(signal));
                                map.put("uprice", String.valueOf(mDeviceInfo.getPrice()));
                                map.put("engcy", "$");
                                String json = mHttpManagementAPI.getMethodPOST(HTTPManagementAPI.WISLINK_URL + "datastats/", map, head);
                                Log.e(TAG, "json    " + json);
                                mProgressDialog.dismiss();

                                ExportActivity.this.finish();
                            } catch (Exception e) {
                                e.printStackTrace();
                                mProgressDialog.dismiss();
                            }
                        }
                    }).start();

                } else {
                    Tips(getString(R.string.tips), getString(R.string.email_illegal)).setPositiveButton(R.string.yes, null).create().show();
                }
            } else {
                Tips(getString(R.string.tips), getString(R.string.enter_mail_account)).setPositiveButton(R.string.yes, null).create().show();
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
            case R.id.send_email:
                if (mDeviceInfo.getPrice() <= 0) {
                    Tips(getString(R.string.tips), getString(R.string.export_device)).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(ExportActivity.this, CostActivity.class);
                            intent.putExtra("serial", mDeviceInfo.getSerial());
                            startActivity(intent);
                        }
                    }).setNegativeButton(R.string.no, null).create().show();
                    return;
                }
                final String email_str = email.getText().toString().trim();
                if (email_str.length() > 0) {
                    Matcher matcher = pattern.matcher(email_str);
                    boolean rs = matcher.matches();
                    if (rs) {
                        mProgressDialog.show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Map<String, String> head = new HashMap<>();
                                    head.put("wislink_auth_uuid", mPhoneInfo.getUuid());
                                    head.put("wislink_auth_token", mPhoneInfo.getToken());
                                    Map<String, String> map = new HashMap<>();
                                    map.put("devuuid", mDeviceInfo.getUuid());
                                    map.put("devname", mDeviceInfo.getName());
                                    map.put("emailaddr", email_str);
                                    map.put("devmac", mDeviceInfo.getMac());
                                    map.put("devstrength", String.valueOf(signal));
                                    map.put("uprice", String.valueOf(mDeviceInfo.getPrice()));
                                    map.put("engcy", "$");
                                    String json = mHttpManagementAPI.getMethodPOST(HTTPManagementAPI.WISLINK_URL + "datastats/", map, head);
                                    Log.e(TAG, "json    " + json);
                                    mProgressDialog.dismiss();

                                    ExportActivity.this.finish();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    mProgressDialog.dismiss();
                                }
                            }
                        }).start();
                    } else {
                        Tips(getString(R.string.tips), getString(R.string.email_illegal)).setPositiveButton(R.string.yes, null).create().show();
                    }
                } else {
                    Tips(getString(R.string.tips), getString(R.string.enter_mail_account)).setPositiveButton(R.string.yes, null).create().show();
                }
                break;
        }
    }


    private CustomDialog.Builder ibuilder = null;

    public CustomDialog.Builder Tips(String title, String msg) {
        ibuilder = new CustomDialog.Builder(ExportActivity.this);
        ibuilder.setTitle(title);
        ibuilder.setMessage(msg);
        return ibuilder;
    }
}
