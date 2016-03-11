package digimagus.csrmesh.acplug;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import digimagus.csrmesh.test.OkHttpUtil;

public class ApplicationTest extends ApplicationTestCase<Application> {
    public static String TAG = "ApplicationTest";

    public ApplicationTest() {
        super(Application.class);
    }


    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ms'Z'");

    private SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void testApp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - timeZone());//标准时间

        String time1=format.format(calendar.getTime());
        Log.e(TAG,"Time1  "+time1);

        String timestamp=String.valueOf(time1).replace('T', ' ').substring(0, time1.indexOf("."));

        Log.e(TAG,"Time2  "+timestamp);

        try {
            calendar.setTime(format1.parse(timestamp));
            calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) + timeZone());//北京时间
            Log.e(TAG,"Time3  "+format.format(calendar.getTime()));

        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    //得到手机本地的时区
    public int timeZone() {
        TimeZone tz = TimeZone.getDefault();
        return Integer.parseInt(createGmtOffsetString(tz.getRawOffset()));
    }


    private String createGmtOffsetString(int offsetMillis) {
        int offsetMinutes = offsetMillis / 60000;
        if (offsetMinutes < 0) {
            offsetMinutes = -offsetMinutes;
        }
        StringBuilder builder = new StringBuilder(9);
        appendNumber(builder, 2, offsetMinutes / 60);
        return builder.toString();
    }

    private void appendNumber(StringBuilder builder, int count, int value) {
        String string = Integer.toString(value);
        for (int i = 0; i < count - string.length(); i++) {
            builder.append('0');
        }
        builder.append(string);
    }



    //同步的GET请求
    public void testGetResponse() throws IOException {

        String url="http://publicobject.com/helloworld.txt";


        OkHttpClient mOkHttpClient=new OkHttpClient();
        mOkHttpClient.setCookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
        mOkHttpClient.setConnectTimeout(30, TimeUnit.SECONDS);
        Request request = new Request.Builder().url(url).build();

        Call call = mOkHttpClient.newCall(request);
        Response execute = call.execute();

        System.out.println(execute.body().string());
    }



    public void testAppGet(){
        OkHttpUtil okHttpUtil=new OkHttpUtil();
        try {
            Log.e(TAG,"testAppGet:  ");
            String data=okHttpUtil.get("http://baidu.com");
            Log.e(TAG,"data:  "+data);
        } catch (IOException e) {
            Log.e(TAG,"data:  "+e.getMessage());
            e.printStackTrace();
        }
    }
}