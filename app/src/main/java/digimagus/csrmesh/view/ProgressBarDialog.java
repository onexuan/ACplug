package digimagus.csrmesh.view;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import digimagus.csrmesh.acplug.R;

/**
 * 仿IOS  菊花加载
 */
public class ProgressBarDialog extends Dialog {
    public static ProgressBarDialog customProgressDialog = null;

    public ProgressBarDialog(Context context) {
        super(context);
    }

    public ProgressBarDialog(Context context, int theme) {
        super(context, theme);
    }

    public ProgressBarDialog createDialog(Context context) {
        customProgressDialog = new ProgressBarDialog(context, R.style.customProgressDialog);
        customProgressDialog.setContentView(R.layout.dialog_progress);
        customProgressDialog.getWindow().getAttributes().gravity = Gravity.CENTER;
        customProgressDialog.setCanceledOnTouchOutside(false);
        return customProgressDialog;
    }

    public ProgressBarDialog setMessage(String strMessage) {
        if (strMessage != null) {
            ((TextView)customProgressDialog.findViewById(R.id.dialog_msg)).setText(strMessage);
        }
        return customProgressDialog;
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }
}
