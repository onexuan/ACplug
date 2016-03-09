package digimagus.csrmesh.acplug;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.digimagus.aclibrary.HTTPManagementAPI;

import java.util.HashMap;
import java.util.Map;

import digimagus.csrmesh.entities.PhoneInfo;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 */
public class NotificationActivity extends Activity implements View.OnClickListener {
    private final static String TAG = "NotificationActivity";
    private ImageView back;
    public ProgressBarDialog mProgressDialog;

    public EditText cost, usage;
    public HTTPManagementAPI mHttpManagementAPI;

    public PhoneInfo mPhoneInfo;
    public String mDevuuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        mProgressDialog = new ProgressBarDialog(this).createDialog(this);
        mProgressDialog.setMessage(getString(R.string.send));
        mPhoneInfo = (PhoneInfo) getIntent().getSerializableExtra("mPhoneInfo");
        mDevuuid = getIntent().getStringExtra("mDevuuid");
        mHttpManagementAPI = HTTPManagementAPI.getInstance();
        initFindViewById();
    }

    private void initFindViewById() {
        back = (ImageView) findViewById(R.id.back);
        cost = (EditText) findViewById(R.id.cost);
        usage = (EditText) findViewById(R.id.usage);
        usage.setOnEditorActionListener(onEditorActionListener);
        back.setOnClickListener(this);
    }

    TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(final TextView v, int actionId, KeyEvent event) {
            mProgressDialog.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String data = v.getText().toString();
                        double d = Double.parseDouble(data);
                        Map<String, String> head = new HashMap<>();
                        head.put("wislink_auth_uuid", mPhoneInfo.getUuid());
                        head.put("wislink_auth_token", mPhoneInfo.getToken());
                        Map<String, String> map = new HashMap<>();
                        map.put("devuuid", mDevuuid);
                        map.put("threshold", String.valueOf(d));
                        String json = mHttpManagementAPI.getMethodPOST(HTTPManagementAPI.WISLINK_URL + "setdevicethr/", map, head);
                        Log.e(TAG, "json    " + json);
                        mProgressDialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                        mProgressDialog.dismiss();
                    }
                }
            }).start();
            return true;
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
        }
    }
}
