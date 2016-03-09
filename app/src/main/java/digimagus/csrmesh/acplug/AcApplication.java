package digimagus.csrmesh.acplug;

import android.app.Application;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;

/**
 *
 */
public class AcApplication extends Application{
    private final static String TAG="AcApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "  onCreate ");

        /*CrashHandler catchHandler = CrashHandler.getInstance();
        catchHandler.init(getApplicationContext());*/

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),    /* get the context for the application */
                "COGNITO_IDENTITY_POOL",    /* Identity Pool ID */
                Regions.US_WEST_1           /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );

    }
}
