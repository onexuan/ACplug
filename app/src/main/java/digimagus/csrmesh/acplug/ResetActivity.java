package digimagus.csrmesh.acplug;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * 重置
 */
public class ResetActivity extends Activity implements View.OnClickListener{
    private final static String TAG="ResetActivity";

    private ProgressBarDialog progressDialog;
    private ImageView back;
    private TextView reset;
    private String serial;
    boolean online=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset);
        serial=getIntent().getStringExtra("serial");
        initFindViewById();
    }

    private void initFindViewById() {
        back= (ImageView)findViewById(R.id.back);
        reset= (TextView)findViewById(R.id.reset);
        progressDialog = new ProgressBarDialog(this).createDialog(this);


        back.setOnClickListener(this);
        reset.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!online){
            Toast.makeText(ResetActivity.this,getString(R.string.device_not_online),Toast.LENGTH_LONG).show();
            reset.setEnabled(false);
        }
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.back:
                finish();
                break;
            case R.id.reset:
                Tips(getString(R.string.reset),getString(R.string.reset_device_tip)).setPositiveButton(R.string.yes,resetListener).setNegativeButton(R.string.no,null).create().show();
                break;
        }
    }

    DialogInterface.OnClickListener resetListener=new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            progressDialog.setMessage(getString(R.string.reset_device)).show();
            handler.postDelayed(timeoutRunnable,10*1000);
            // controller.resetDevice(serial);
        }
    };

    Handler handler=new Handler();
    Runnable timeoutRunnable=new Runnable() {
        @Override
        public void run() {
            progressDialog.dismiss();
            handler.removeCallbacks(timeoutRunnable);
            Tips(getString(R.string.tips),getString(R.string.timeout)).setPositiveButton(R.string.yes,null).create().show();
        }
    };




    private CustomDialog.Builder ibuilder = null;
    public CustomDialog.Builder Tips(String title, String msg) {
        ibuilder = new CustomDialog.Builder(ResetActivity.this);
        ibuilder.setTitle(title);
        ibuilder.setMessage(msg);
        return ibuilder;
    }
}
