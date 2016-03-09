package digimagus.csrmesh.acplug;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.digimagus.aclibrary.MessageService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * 欢迎界面
 */
public class WelcomeActivity extends BaseActivity {

    private boolean data_exist = false;
    /**
     * Android 5.0 之后 Service 必须采用显示启动
     * Android 5.0 之前可以采用隐式和显示
     */

    @Override
    protected void initContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_welcome);
        Intent mIntent = new Intent("snail.digimagus.csrmesh.acplug");
        Intent eintent = new Intent(getExplicitIntent(this,mIntent));
        startService(eintent);
    }

    private Intent getExplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);
        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }
        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);
        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);
        // Set the component to be explicit
        explicitIntent.setComponent(component);
        return explicitIntent;
    }


    @Override
    protected void handler(Message msg) {
        try {
            JSONObject obj = new JSONObject(String.valueOf(msg.obj));
            switch (msg.what) {
                case PHONE_FIND_DEVICE://接收到UDP
                    int type = obj.getInt("type");
                    int firm = obj.getInt("firm");
                    Log.e(TAG, "UDP : " + obj);
                    if (type != 100301 && firm != 150002) {
                        json = null;
                        return;
                    }
                    String ip = obj.getString("IP");
                    if (ip.equals("192.168.4.1")) {
                        data_exist = false;
                    } else {
                        data_exist = true;
                    }
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String json = null;
    Handler handler = new Handler();
    private long current = 0;

    Runnable udpRunable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - current < MessageService.UDP_DELAYED_5000) {
                handler.postDelayed(udpRunable, 1000);
            } else {
                handler.removeCallbacks(udpRunable);
                if (data_exist) {
                    Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(WelcomeActivity.this, ChooseActivity.class);
                    intent.putExtra("first_time", true);
                    startActivity(intent);
                    finish();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        data_exist = mDeviceStore.loadAllInfo();
        current = System.currentTimeMillis();
        handler.post(udpRunable);
    }

    private final static String TAG = "WelcomeActivity";
}
