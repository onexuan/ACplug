package digimagus.csrmesh.acplug;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class TutorialActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
    }

    @Override
    protected void onStart() {
        super.onStart();
        /*Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("file*//*.json");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "csrmesh");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "csrmesh");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(getIntent().getStringExtra("crash")));
        startActivity(Intent.createChooser(shareIntent, "csrmesh"));*/
    }
}
