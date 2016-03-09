package com.digimagus.aclibrary;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


/**
 * HTTP 请求管理API
 *
 * @author wangll@digimagus.com
 */
public class HTTPManagementAPI {
    /**
     * WISLINK 服务器 URL
     */
    public static final String WISLINK_URL = "http://52.74.130.80:8080/v1/";
    /**
     * 谷歌定位的 URL
     */
    public static final String LOCATION_URL = "http://maps.google.cn/maps/api/geocode/json";
    /**
     * Log
     */
    private final static String TAG = "HTTPManagementAPI";

    /**
     * 手机定位成功
     */
    public final static int PHONE_LOCATE_SUCCESS = 605;

    /**
     * 手机定位失败
     */
    public final static int PHONE_LOCATE_FAILED = 606;

    /**
     * 绑定设备Binding
     */
    public static final int BINDING_DEVICE = 303;

    /**
     * 单例模式
     *
     * Http 请求工具类
     */
    private static class LazyHolder {
        private static final HTTPManagementAPI INSTANCE = new HTTPManagementAPI();
    }

    public static final HTTPManagementAPI getInstance() {
        return LazyHolder.INSTANCE;
    }

    public HTTPManagementAPI() {
    }

    /**
     * HTTP 相关参数请求封装
     *
     * @param urlString  url 地址
     * @param MethodType 请求的类型 （GET、POST、PUT）
     * @return HttpURLConnection 对象
     * @throws IOException 返回异常信息
     */
    public HttpURLConnection conn(String urlString, String MethodType) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(8000 /* milliseconds */);
        conn.setConnectTimeout(8000 /* milliseconds */);
        conn.setRequestMethod(MethodType);
        return conn;
    }

    /**
     * HTTP  PUT方法参数
     *
     * @param url    url地址
     * @param head   头部消息
     * @param params 发送参数
     * @return 返回PUT请求结果
     * @throws Exception 返回异常信息
     */
    public String getMethodPUT(String url, Map<String, String> head, Map<String, String> params) throws Exception {
        HttpURLConnection conn = conn(url, "PUT");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("wislink_auth_uuid", head.get("wislink_auth_uuid"));
        conn.setRequestProperty("wislink_auth_token", head.get("wislink_auth_token"));
        conn.connect();
        if (params != null) {
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            String map = "";
            for (String key : params.keySet()) {
                map = map + key + "=" + params.get(key) + "&";
            }
            if (!"".equals(map)) {
                map = map.substring(0, map.lastIndexOf("&"));
            }
            out.writeBytes(map);
            out.flush();
            out.close();
        }
        Log.e(TAG, "ResponseCode :   " + conn.getResponseCode());
        InputStream stream = conn.getInputStream();
        return getInputStream(stream);
    }


    /**
     * HTTP POST 方法请求
     *
     * @param url    请求地址连接
     * @param params 请求参数
     * @return 返回POST请求结果
     * @throws Exception 返回异常信息
     */
    public String getMethodPOST(String url, Map<String, String> params, Map<String, String> head) throws Exception {
        HttpURLConnection conn = conn(url, "POST");
        if (head != null) {
            conn.setRequestProperty("wislink_auth_uuid", head.get("wislink_auth_uuid"));
            conn.setRequestProperty("wislink_auth_token", head.get("wislink_auth_token"));
        }
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.connect();
        if (params != null) {
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            String map = "";
            for (String key : params.keySet()) {
                map = map + key + "=" + params.get(key) + "&";
            }
            if (!"".equals(map)) {
                map = map.substring(0, map.lastIndexOf("&"));
            }
            out.writeBytes(map);
            out.flush();
            out.close();
        }
        return getInputStream(conn.getInputStream());
    }

    /**
     * @param url    请求的http 地址
     * @param params 请求的参数 1、头部消息   2、参数
     * @param type   请求类型 1、查询设备的状态  2、普通GET
     * @return 返回GET数据
     * @throws Exception 返回异常信息
     */
    public String getMethodGET(String url, Map<String, String> params, boolean type) throws Exception {
        HttpURLConnection conn = null;
        if (type) {
            conn = conn(url, "GET");
            conn.setRequestProperty("Content-type", "application/x-java-serialized-object");
            conn.setRequestProperty("wislink_auth_uuid", params.get("wislink_auth_uuid"));
            conn.setRequestProperty("wislink_auth_token", params.get("wislink_auth_token"));
        } else {
            if (params != null) {
                String map = "";
                for (String key : params.keySet()) {
                    map = map + key + "=" + params.get(key) + "&";
                }
                if (!"".equals(map)) {
                    url = url + "?" + map.substring(0, map.lastIndexOf("&"));
                }
            }
            conn = conn(url, "GET");
        }
        conn.connect();
        return getInputStream(conn.getInputStream());
    }

    private String getInputStream(InputStream stream) throws Exception {
        byte[] data = new byte[stream.available()];
        Log.e(TAG,"SIZE: "+stream.available());
        int length = 0;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        while ((length = stream.read(data)) != -1) {
            bout.write(data, 0, length);
        }
        String dat = new String(bout.toByteArray(), "UTF-8");
        stream.close();
        return dat;
    }

    /**
     * 定位
     *
     * @param context 上下文对象
     * @param handler handler 消息
     */
    public void startLocate(Context context, final Handler handler) {
        final Location location = getLocation(context);
        if (location == null) {
            Message message = handler.obtainMessage();
            message.what = HTTPManagementAPI.PHONE_LOCATE_FAILED;
            handler.sendMessage(message);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Map<String, String> map = new HashMap<>();
                    map.put("latlng", location.getLatitude() + "," + location.getLongitude());
                    map.put("sensor", true + "");
                    Log.e(TAG, "LOCATION :  URL:" + LOCATION_URL + " " + location.getLatitude() + "," + location.getLongitude());
                    try {
                        String json = getMethodGET(LOCATION_URL, map, false);

                        JSONObject obj = new JSONObject(json);
                        if (obj.getString("status").equals("OK")) {
                            JSONArray array = obj.getJSONArray("results");
                            int length = array.length();
                            obj = array.getJSONObject(2);
                            String geography = obj.getString("formatted_address");
                            obj = array.getJSONObject(3);
                            JSONArray arr = obj.getJSONArray("address_components");
                            String city = arr.getJSONObject(0).getString("long_name");
                            obj = array.getJSONObject(length-1);
                            arr = obj.getJSONArray("address_components");
                            String NO = arr.getJSONObject(0).getString("short_name");
                            Message message = handler.obtainMessage();
                            message.what = HTTPManagementAPI.PHONE_LOCATE_SUCCESS;
                            message.obj = NO + "," + geography + "," + city;
                            handler.sendMessage(message);
                            //定位成功
                        } else {
                            //定位失败
                            Message message = handler.obtainMessage();
                            message.what = HTTPManagementAPI.PHONE_LOCATE_FAILED;
                            handler.sendMessage(message);

                            Log.e(TAG, "LOCATION-C :  " + json);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Message message = handler.obtainMessage();
                        message.what = HTTPManagementAPI.PHONE_LOCATE_FAILED;
                        handler.sendMessage(message);

                        Log.e(TAG, "LOCATION-E :  ");
                    }

                }
            }).start();
        }
    }


    /**
     * 4.获取用户的当前位置
     * 1.在AndroidManifest.xml当中声明相应的权限
     * 2.获取LocationManager对象
     * 3.选择LocationProvider
     * 4.绑定LocationListener对象
     *
     * @param context 上下文对象
     */
    public Location getLocation(Context context) {
        LocationManager locMan = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        Location location = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        return location;
    }

    private int checkSelfPermission(String accessFineLocation) {
        return 0;
    }
}