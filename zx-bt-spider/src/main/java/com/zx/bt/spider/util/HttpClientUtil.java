package com.zx.bt.spider.util;

import com.zx.bt.spider.config.Config;
import com.zx.bt.common.exception.BTException;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * author:ZhengXing
 * datetime:2017/11/7 0007 15:43
 * httpClient工具类
 */
@Slf4j
@Component
public class HttpClientUtil {
    private static final String LOG = "[HttpClientUtil]";


    //连接池对象
    private PoolingHttpClientConnectionManager pool = null;

    //请求配置
    private RequestConfig requestConfig;

    /**
     * 发送post请求，返回String
     */
    public <T> String doPostForString(String url, T obj){
        CloseableHttpResponse response = null;
        String result;
        try {
            //发送请求返回response
            response = doPost(url, obj);
            //response 转 string
            result = responseToString(response);
        } finally {
            //关闭
            closeResponseAndIn(null,response);
        }
        return result;
    }

    /**
     * 发送post请求,使用json数据,返回string
     */
    public String doPostForString(String url, String jsonString){
        CloseableHttpResponse response = null;
        try {
            //发送请求返回response
            response = doPost(url, jsonString);
            //response 转 string
            return responseToString(response);
        } finally {
            //关闭
            closeResponseAndIn(null,response);
        }
    }

    /**
     * 根据请求url/headers/entity构造 httppost
     */
    public HttpPost buildHttpPost(String url, Header[] headers, HttpEntity entity) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(headers);
        httpPost.setEntity(entity);
        return httpPost;
    }

    /**
     * 根据请求url/headers构造 httppost
     */
    public HttpGet buildHttpGet(String url, Header[] headers) {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeaders(headers);
        return httpGet;
    }

    /**
     * 构建一个简单的有基本header信息的request发起get请求
     */
    public String doGetForBasicBrowser(String url) {
        HttpGet httpGet = buildHttpGet(url, new BasicHeader[]{
                new BasicHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36")});
        return doRequest(httpGet);
    }

    /**
     * 发送请求并返回string结果,根据httppost或httpget
     */
    public String doRequest(HttpRequestBase httpRequest) {
        CloseableHttpResponse response = null;
        try {
           response = execute(httpRequest);
            return responseToString(response);
        }
        catch (BTException e){
//            log.error("{}请求异常:{}",LOG,e.getMessage());
            throw e;
        }
        finally {
            //关闭
            closeResponseAndIn(null,response);
        }
    }


    /**
     * get请求返回 string结果
     */
    public String doGet(String url) {
        HttpGet httpGet = new HttpGet( url);
        return doRequest(httpGet);
    }




    /**
     * 发起post请求,根据url，参数实体
     */
    public <T> CloseableHttpResponse doPost(String url, T obj) {
        Map<String, String> map = objectToMap(obj);
        //遍历map将其将如paramList
        List<NameValuePair> params = new ArrayList<>();
        for(Map.Entry<String,String> item : map.entrySet()){
            params.add(new BasicNameValuePair(item.getKey(),item.getValue()));
        }
        //放入请求参数中
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

        return execute(httpPost);
    }


    /**
     * 发起post请求,json格式
     */
    public CloseableHttpResponse doPost(String url, String jsonString) {
        //放入请求参数中
        HttpPost httpPost = new HttpPost(url);
        if (StringUtils.isNotBlank(jsonString)) {
            StringEntity stringEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
            stringEntity.setContentEncoding("UTF-8");
            httpPost.setEntity(stringEntity);
        }

        return execute(httpPost);
    }



    /**
     * 对象转map
     * @param obj
     * @param <T>
     * @return
     */
    private <T> Map<String, String> objectToMap(T obj) {
        //实体类转map
        Map<String, String> map = null;
        try {
            map = BeanUtils.describe(obj);
            //会产生key为class的元素
            map.remove("class");
        } catch (IllegalAccessException |InvocationTargetException |NoSuchMethodException e) {
            throw new BTException(LOG + "objectToMap异常:" + e.getMessage());
        }
        return map;
    }

    /**
     * 执行请求并返回响应
     */
    private CloseableHttpResponse execute(HttpUriRequest request) {
        CloseableHttpResponse response;
        try {
            response = getHttpClient().execute(request);
        } catch (Exception e) {
            throw new BTException(LOG + "execute异常:" + e.getMessage());
        }
        return response;
    }


    /**
     * 从response 中取出 html String
     * 如果没有访问成功，返回null
     */
    public String responseToString(CloseableHttpResponse response) {
        if (isSuccess(response)) {
            try {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            } catch (IOException e) {
                throw new BTException(LOG + "responseToString异常:" + e.getMessage());
            }
        }
        //这句不可能执行到...，返回值不会为null
        return null;
    }

    /**
     * 校验是否请求成功
     */
    private boolean isSuccess(CloseableHttpResponse response) {
        boolean flag = null != response && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        //成功直接返回
        if(flag)
            return flag;

        //如果失败，记录日志，关闭response，抛出异常
        closeResponseAndIn(null, response);
        throw new BTException(LOG + "失败,当前状态码:" + (response == null ? "null" : response.getStatusLine().getStatusCode()));
    }



    /**
     * 关闭  in 和 response
     */
    public void closeResponseAndIn(InputStream inputStream, CloseableHttpResponse response) {
        try {
            @Cleanup
			InputStream temp1 = inputStream;
            @Cleanup
            CloseableHttpResponse temp2 = response;
        } catch (Exception e) {
            //不抛出异常
        }
    }

    /**
     * 获取HttpClient
     */
    public CloseableHttpClient getHttpClient() {
        return HttpClients.custom()
                //设置连接池
                .setConnectionManager(pool)
                //请求配置
                .setDefaultRequestConfig(requestConfig)
                .build();
    }



    /**
     * 私有化构造方法，构造时，创建对应的连接池实例
     * 使用连接池管理HttpClient可以提高性能
     * @param config
     */
    public HttpClientUtil(Config config) {
        try {
            /**
             * 初始化连接池
             */
            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                    sslContextBuilder.build());
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", socketFactory)
                    .register("http", new PlainConnectionSocketFactory())
                    .build();
            pool = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            pool.setMaxTotal(config.getHttp().getMaxConnectionNum());
            pool.setDefaultMaxPerRoute(config.getHttp().getMaxPerRoute());

            /**
             * 初始化请求配置
             */
            requestConfig = RequestConfig.custom()
                    .setSocketTimeout(config.getHttp().getSocketTimeout())
                    .setConnectionRequestTimeout(config.getHttp().getConnectionRequestTimeout())
                    .setConnectTimeout(config.getHttp().getConnectionTimeout())
                    .build();
        } catch (Exception e) {
            throw new BTException(LOG + "初始化异常" + e.getMessage());
        }
    }


}
