package digimagus.csrmesh.acplug.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.digimagus.aclibrary.MessageService;

import digimagus.csrmesh.acplug.ChooseActivity;
import digimagus.csrmesh.acplug.HideDeviceActivity;
import digimagus.csrmesh.acplug.LocationActivity;
import digimagus.csrmesh.acplug.PrivacyActivity;
import digimagus.csrmesh.acplug.R;
import digimagus.csrmesh.acplug.TermsActivity;
import digimagus.csrmesh.acplug.TutorialActivity;
import digimagus.csrmesh.acplug.listener.FragmentController;
import digimagus.csrmesh.entities.Setting;
import digimagus.csrmesh.view.CustomDialog;
import digimagus.csrmesh.view.ProgressBarDialog;

/**
 * 更多设备
 */
public class MoreFragment extends Fragment implements View.OnClickListener {
    private final static String TAG = "MoreFragment";
    private View contextView;
    private RelativeLayout add_new_device, location, setup_guide, terms, ratethisapp, reportproblem, add_hide_device, privacy;
    private TextView app_version, location_txt;
    private FragmentController mFragmentController;
    public Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contextView = inflater.inflate(R.layout.fragment_more, container, false);
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
    public void onStart() {
        super.onStart();
        Setting setting = mFragmentController.getSetting();
        if (setting != null && setting.getCity() != null) {
            location_txt.setText(setting.getLocate());
        }
    }

    private void initFindViewById() {
        add_new_device = (RelativeLayout) contextView.findViewById(R.id.add_new_device);
        location = (RelativeLayout) contextView.findViewById(R.id.location);
        privacy = (RelativeLayout) contextView.findViewById(R.id.privacy);
        setup_guide = (RelativeLayout) contextView.findViewById(R.id.setup_guide);
        terms = (RelativeLayout) contextView.findViewById(R.id.terms);
        ratethisapp = (RelativeLayout) contextView.findViewById(R.id.ratethisapp);
        reportproblem = (RelativeLayout) contextView.findViewById(R.id.reportproblem);
        add_hide_device = (RelativeLayout) contextView.findViewById(R.id.add_hide_device);
        app_version = (TextView) contextView.findViewById(R.id.app_version);
        location_txt = (TextView) contextView.findViewById(R.id.location_txt);
        app_version.setText(mFragmentController.appVersion());

        add_new_device.setOnClickListener(this);
        location.setOnClickListener(this);
        setup_guide.setOnClickListener(this);
        terms.setOnClickListener(this);
        ratethisapp.setOnClickListener(this);
        reportproblem.setOnClickListener(this);
        add_hide_device.setOnClickListener(this);
        privacy.setOnClickListener(this);
    }

    private CustomDialog.Builder ibuilder;

    private CustomDialog.Builder Tips(String title, String msg) {
        ibuilder = new CustomDialog.Builder(context);
        ibuilder.setTitle(title);
        ibuilder.setMessage(msg);
        return ibuilder;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_new_device:
                //MessageService.CURRENT_NETWORK_TYPE
                /*if (MessageService.CURRENT_NETWORK_TYPE == MessageService.CONN_NETWORK_TYPE_WIFI && MessageService.CONN_NETWORK_IS_SERVER) {
                    mFragmentController.jumpActivity(ChooseActivity.class, null);
                } else if (MessageService.CURRENT_NETWORK_TYPE != MessageService.CONN_NETWORK_TYPE_WIFI) {
                    Tips(getString(R.string.tips), getString(R.string.not_connected_to_the_wifi)).setPositiveButton(R.string.yes, null).create().show();
                } else if (!MessageService.CONN_NETWORK_IS_SERVER) {
                    Tips(getString(R.string.tips), getString(R.string.not_connected_to_the_server)).setPositiveButton(R.string.yes, null).create().show();
                }*/
                mFragmentController.jumpActivity(ChooseActivity.class, null);
                break;
            case R.id.add_hide_device:
                mFragmentController.jumpActivity(HideDeviceActivity.class, null);
                break;
            case R.id.location:
                mFragmentController.jumpActivity(LocationActivity.class, null);
                break;
            case R.id.setup_guide:
                mFragmentController.jumpActivity(TutorialActivity.class, null);
                break;
            case R.id.privacy:
                mFragmentController.jumpActivity(PrivacyActivity.class, null);
                break;
            case R.id.terms:
                mFragmentController.jumpActivity(TermsActivity.class, null);
                break;
            case R.id.ratethisapp:
                //mFragmentController.jumpActivity(WebStoreActivity.class, null);
                final String appPackageName = "com.tencent.mm"/*"digimagus.csrmesh.acplug"*/; // getPackageName() from Context or Activity object
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                break;
            case R.id.reportproblem:
                try {
                    Intent data = new Intent(Intent.ACTION_SENDTO);
                    data.setData(Uri.parse("mailto:wangll@digimagus.com"));
                    data.putExtra(Intent.EXTRA_SUBJECT, "title");
                    data.putExtra(Intent.EXTRA_TEXT, "content");
                    startActivity(data);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, getString(R.string.report_email), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}