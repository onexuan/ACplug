package digimagus.csrmesh.test;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class OkHttpUtil {
    OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public String get(String url) throws IOException {
        Request.Builder request = new Request.Builder();
        request.url(url);


        Response response = client.newCall(request.build()).execute();
        return response.body().string();
    }

    public String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request.Builder request = new Request.Builder();
        request.url(url);


        Response response = client.newCall(request.build()).execute();
        return response.body().string();
    }

    public String put(String url,String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request.Builder request = new Request.Builder();
        request.url(url);


        Response response = client.newCall(request.build()).execute();
        return  response.body().string();
    }

    public String delete(String url,String json) throws  IOException{
        RequestBody body = RequestBody.create(JSON, json);
        Request.Builder request = new Request.Builder();
        request.url(url);


        Response response = client.newCall(request.build()).execute();
        return  response.body().string();
    }
}