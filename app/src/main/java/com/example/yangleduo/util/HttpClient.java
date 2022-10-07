package com.example.yangleduo.util;

import android.text.TextUtils;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.request.GetRequest;
import com.lzy.okgo.request.PostRequest;

import okhttp3.OkHttpClient;

/**
 * Created by 123 on 2019/9/23.
 */

public class HttpClient {

  private static final long TIMEOUT = 10000;
  private static HttpClient sInstance;
  private OkHttpClient mOkHttpClient;
  private String mLanguage;//语言
  private String mUrl;

  private HttpClient() {
    System.out.println("httpClient");
  }

  public static HttpClient getInstance() {
    if (sInstance == null) {
      synchronized (HttpClient.class) {
        if (sInstance == null) {
          sInstance = new HttpClient();
        }
      }
    }
    return sInstance;
  }


  public GetRequest get(String serviceName, String baseUrl) {
    if(TextUtils.isEmpty(baseUrl)) baseUrl = mUrl;
    return OkGo.get(baseUrl + serviceName)
            .headers("Connection","keep-alive")
            .tag(serviceName)
            .params("language", mLanguage);
  }

  public PostRequest post(String serviceName, String baseUrl) {
    if(TextUtils.isEmpty(baseUrl)) baseUrl = mUrl;
    return OkGo.post(baseUrl + serviceName)
            .headers("Connection","keep-alive")
            .tag(serviceName)
            .params("language", mLanguage);
  }

  public void cancel(String tag) {
    OkGo.cancelTag(mOkHttpClient, tag);
  }

  public void setLanguage(String language) {
    mLanguage = language;
  }

}