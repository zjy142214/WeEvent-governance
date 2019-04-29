package com.webank.weevent.governance.configuration;

import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * http连接池配置
 * @author piper
 * @date 2018/7/4 13:15
 */
@Configuration
public class ConnectionManager {

   // 最大连接数
   private static final int MAX_TOTAL = 200;
   // 每一个路由的最大连接数
   private static final int MAX_PER_ROUTE = 500;
   //从连接池中获得连接的超时时间
   private static final int CONNECTION_REQUEST_TIMEOUT = 3000;
   //连接超时
   private static final int CONNECTION_TIMEOUT = 3000;
   //获取数据的超时时间
   private static final int SOCKET_TIMEOUT = 5000;

   private PoolingHttpClientConnectionManager cm;
   private CloseableHttpClient httpClient;

   /**
    * 重连接策略
    */
   HttpRequestRetryHandler retryHandler = (exception, executionCount,context) -> {
       if (executionCount >= 3) {
           // Do not retry if over max retry count
           return false;
       }
       if (exception instanceof InterruptedIOException) {
           // Timeout
           return false;
       }
       if (exception instanceof UnknownHostException) {
           // Unknown host
           return false;
       }
       if (exception instanceof ConnectTimeoutException) {
           // Connection refused
           return false;
       }
       if (exception instanceof SSLException) {
           // SSL handshake exception
           return false;
       }
              
       HttpClientContext clientContext = HttpClientContext.adapt(context);
       HttpRequest request = clientContext.getRequest();
       boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
       if (idempotent) {
           // Retry if the request is considered idempotent
           return true;
       }
       return false;
   };

   /**
    * 配置连接参数
    */
   RequestConfig requestConfig = RequestConfig.custom()
       .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
       .setConnectTimeout(CONNECTION_TIMEOUT)
       .setSocketTimeout(SOCKET_TIMEOUT)
       .build();

   public ConnectionManager() {
       cm = new PoolingHttpClientConnectionManager();
       cm.setMaxTotal(MAX_TOTAL);
       cm.setDefaultMaxPerRoute(MAX_PER_ROUTE);

       // 定制实现HttpClient，全局只有一个HttpClient
       httpClient = HttpClients.custom()
           .setConnectionManager(cm)
           .setDefaultRequestConfig(requestConfig)
           .setRetryHandler(retryHandler)
           .build();
   }

   @Bean
   public CloseableHttpClient getHttpClient() {
       return httpClient;
   }

}

