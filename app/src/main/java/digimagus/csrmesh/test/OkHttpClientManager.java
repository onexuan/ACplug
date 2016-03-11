package digimagus.csrmesh.test;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

/**
 *
 * OkHttp管理类
 *
 */
public class OkHttpClientManager {
    //GET、POST、PUT

    OkHttpClient mOkHttpClient=new OkHttpClient();

    public OkHttpClientManager() {
        mOkHttpClient.setCookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
        mOkHttpClient.setConnectTimeout(5*1000, TimeUnit.SECONDS);
    }

    //同步的GET请求
    public void getResponse() throws IOException {

        String url="www.baidu.com";

        mOkHttpClient.setCookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER));
        mOkHttpClient.setConnectTimeout(30, TimeUnit.SECONDS);
        Request request = new Request.Builder().url(url).build();

        Call call = mOkHttpClient.newCall(request);
        Response execute = call.execute();

        System.out.println(execute.body().string());
    }
}
