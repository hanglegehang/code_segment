package com.jdai.task.utils

import org.apache.http.HttpEntity
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.NoHttpResponseException
import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.config.Registry
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.LayeredConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils

import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
/**
 *      compile group: 'org.apache.httpcomponents', name: 'httpclient', version: '4.5.2'
 *      compile group: 'org.apache.httpcomponents', name: 'httpmime', version: '4.5.2'
 *      compile group: 'org.apache.httpcomponents', name: 'fluent-hc', version: '4.5.2'
 */
  class HttpClientUtil {
    static final int timeOut = 10 * 1000;

    private static CloseableHttpClient httpClient = null;

    private final static Object syncLock = new Object();

    private static void config(HttpRequestBase httpRequestBase) {
        httpRequestBase.setHeader("Cookie", "JSESSIONID=5B6B3E96FEA6C89DB0C7498DCCDC3368; sso.jd.com=79253f5ada834adc9a5e7bb2c0ca26cd");
        // 配置请求的超时设置
        RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(timeOut)
        .setConnectTimeout(timeOut).setSocketTimeout(timeOut).build();
        httpRequestBase.setConfig(requestConfig);
    }
    /**
     *
     * @author lihang35
     * @date 2018/5/2 上午9:36
     * @param
     * @return
     */
     public static CloseableHttpClient getHttpClient(String url) {
        String hostname = url.split("/")[2];
        int port = 80;
        if (hostname.contains(":")) {
            String[] arr = hostname.split(":");
            hostname = arr[0];
            port = Integer.parseInt(arr[1]);
        }
        if (httpClient == null) {
            synchronized (syncLock) {
                if (httpClient == null) {
                    httpClient = createHttpClient(200, 40, 100, hostname, port);
                }
            }
        }
        return httpClient;
    }

    /**
     * 创建HttpClient对象
     * @author lihang35
     * @date 2018/5/2 上午9:45
     * @param
     * @return
     */
     public
     static CloseableHttpClient createHttpClient(int maxTotal, int maxPerRoute, int maxRoute, String hostname, int port) {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
        .register("http", plainsf)
        .register("https", sslsf).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        // 将最大连接数增加
        cm.setMaxTotal(maxTotal);
        // 将每个路由基础的连接增加
        cm.setDefaultMaxPerRoute(maxPerRoute);
        HttpHost httpHost = new HttpHost(hostname, port);
        // 将目标主机的最大连接数增加
        cm.setMaxPerRoute(new HttpRoute(httpHost), maxRoute);
        // 请求重试处理
        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (executionCount >= 5) {// 如果已经重试了5次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// 超时
                    return false;
                }
                if (exception instanceof UnknownHostException) {// 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {// SSL握手异常
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext
                .adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }

        };
        ConnectionKeepAliveStrategy keepAliveStrategy = new DefaultConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                long keepAlive = super.getKeepAliveDuration(response, context);
                if (keepAlive == -1) {
                    //如果服务器没有设置keep-alive这个参数，我们就把它设置成5秒
                    keepAlive = 5000;
                }
                return keepAlive;
            }
        };
        HttpHost proxy = new HttpHost("127.0.0.1", 8888);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
        CloseableHttpClient httpClient = HttpClients.custom()
//                .setRoutePlanner(routePlanner)
.setConnectionManager(cm)
.setKeepAliveStrategy(keepAliveStrategy)
.setRetryHandler(httpRequestRetryHandler).build()
return httpClient;
}

    /**
     * POTS请求URL获取内容
     *
     * @param url
     * @return
     */
     public static String post(String url, Map<String, Object> params) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        config(httpPost);
        setPostParams(httpPost, params);
        CloseableHttpResponse response = null;
        try {
            response = getHttpClient(url).execute(httpPost, HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
            return result;
            } catch (Exception e) {
                throw e;
                } finally {
                    try {
                        if (response != null)
                        response.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

    /**
     * POTS请求URL获取内容
     *
     * @param url
     * @return
     */
     public static String post(String url, String params, boolean isBody) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        config(httpPost);
        //设置参数
        StringEntity stringEntity = new StringEntity(params, "UTF-8");
        if (isBody) {
            stringEntity.setContentType("application/json");
            } else {
                stringEntity.setContentType("application/x-www-form-urlencoded");
            }
            httpPost.setEntity(stringEntity);

            CloseableHttpResponse response = null;
            try {
                response = getHttpClient(url).execute(httpPost, HttpClientContext.create());
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity, "utf-8");
                EntityUtils.consume(entity);
                return result;
                } catch (Exception e) {
                    throw e;
                    } finally {
                        try {
                            if (response != null)
                            response.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    private static void setPostParams(HttpPost httpost, Map<String, Object> params) {
                        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                        Set<String> keySet = params.keySet();
                        for (String key : keySet) {
                            nvps.add(new BasicNameValuePair(key, params.get(key).toString()));
                        }
                        try {
                            httpost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }

    /**
     * GET请求URL获取内容
     *
     * @param url
     * @return
     * @author SHANHY
     * @create 2015年12月18日
     */
     public static String get(String url) {
        HttpGet httpGet = new HttpGet(url);
        config(httpGet);
        CloseableHttpResponse response = null;
        try {
            response = getHttpClient(url).execute(httpGet, HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
            return result;
            } catch (IOException e) {
                e.printStackTrace();
                } finally {
                    try {
                        if (response != null)
                        response.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }

                public static void main(String[] args) {
        // URL列表数组
        String[] urisToGet = [
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "http://blog.csdn.net/catoop/article/details/38849497",
        "https://www.baidu.com",
        "http://blog.csdn.net/catoop/article/details/38849497"
        ]

        long start = System.currentTimeMillis();
        try {
            int pagecount = urisToGet.length;
            ExecutorService executors = Executors.newFixedThreadPool(pagecount);
            CountDownLatch countDownLatch = new CountDownLatch(pagecount);
            for (int i = 0; i < pagecount; i++) {
                HttpGet httpget = new HttpGet(urisToGet[i]);
                config(httpget);
                // 启动线程抓取
                executors.execute(new GetRunnable(urisToGet[i], countDownLatch));
            }
            countDownLatch.await();
            executors.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
                } finally {
                    System.out.println("线程" + Thread.currentThread().getName() + ","
                        + System.currentTimeMillis() + ", 所有线程已完成，开始进入下一步！");
                }

                long end = System.currentTimeMillis();
                System.out.println("consume -> " + (end - start));
            }

            static class GetRunnable implements Runnable {
                private CountDownLatch countDownLatch;
                private String url;

                GetRunnable(String url, CountDownLatch countDownLatch) {
                    this.url = url;
                    this.countDownLatch = countDownLatch;
                }

                @Override
                public void run() {
                    try {
                        println url
                        HttpClientUtil.get(url);
                        } finally {
                            countDownLatch.countDown();
                        }
                    }
                }


            }
