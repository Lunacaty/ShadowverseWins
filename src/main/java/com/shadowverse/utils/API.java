package com.shadowverse.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;



import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.htmlunit.jetty.util.IO;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class API {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String LAST_URL = null;
    
    public String serverUrl = null;
    CookieStore cookieStore = new BasicCookieStore();
    private static final Semaphore semaphore = new Semaphore(30);
    
    private CloseableHttpClient client = HttpClients.custom()
            .setDefaultCookieStore(cookieStore)
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectTimeout(5000)
                    .setSocketTimeout(5000)
                    .build())
            .build();
    
    @SneakyThrows
    public void setProxy(){
        String proxyHost = "127.0.0.1";
        int proxyPort = 7897;
        HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        
        SSLContext sslContext = SSLContextBuilder.create()
                .build();
        
        // 2. 配置支持的TLS协议（优先使用TLSv1.2/1.3，避免协议不兼容）
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.2", "TLSv1.3"}, // 服务器支持的协议版本
                null,
                NoopHostnameVerifier.INSTANCE // 不验证主机名（可选，视服务器配置）
        );
        
        client = HttpClients.custom()
               .setDefaultCookieStore(cookieStore)
                .setSSLSocketFactory(sslSocketFactory)
               .setDefaultRequestConfig(RequestConfig.custom()
                       .setCookieSpec(CookieSpecs.STANDARD)
                       .setConnectTimeout(5000)
                       .setSocketTimeout(5000)
                       .setProxy(proxy)
                       .build())
               .build();
    }
    
    /**
     * 使用Apache HttpClient下载图片并调整大小
     * @param imageUrl 图片URL
     * @param savePath 保存路径
     * @return 是否成功
     */
    public boolean downloadImage(
            String imageUrl,
            String savePath) {
        
        HttpGet httpGet = new HttpGet(imageUrl);
        CloseableHttpResponse response = null;
        InputStream inputStream = null;
        
        try {
            semaphore.acquire();
            for(int i = 0; i < 3; i++) {
                // 1. 发送HTTP请求获取图片流
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
                httpGet.setHeader("Accept", "image/webp,image/*,*/*;q=0.8");
                try {
                    response = client.execute(httpGet);
                } catch (Exception e) {
                    System.out.println("下载失败,尝试第" + (i + 1) + "次，异常: " + e.getMessage());
                    if(i == 2){
                        System.out.println("下载失败,尝试次数过多，放弃下载");
                        semaphore.release();
                        return false;
                    }
                    continue;
                }
                
                // 检查响应状态
                if (response.getStatusLine().getStatusCode() == 200) {
                    break;
                }
                System.out.println("下载失败,尝试第" + (i + 1) + "次，状态码: " + response.getStatusLine().getStatusCode());
                if(i == 2){
                    System.out.println("下载失败,尝试次数过多，放弃下载");
                    semaphore.release();
                    return false;
                }
            }
            semaphore.release();
            // 2. 读取图片流
            inputStream = response.getEntity().getContent();
            
            // 4. 保存图片
            File outputFile = new File(savePath + "/" + imageUrl.substring(28));
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs(); // 自动创建目录
            }
            
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            return true;
            
        } catch (Exception e) {
            System.out.println("处理失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // 关闭资源
            try {
                if (inputStream != null) inputStream.close();
                if (response != null) response.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @SneakyThrows
    public Response GET(String url, String... params) {
        
        URIBuilder uriBuilder = new URIBuilder(serverUrl + url);
        
        if (params.length > 0) {
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    uriBuilder.addParameter(keyValue[0], keyValue[1]);
                } else {
                    uriBuilder.addParameter(param, "");
                }
            }
        }
        
        LAST_URL = uriBuilder.build().toString();
//        System.out.println(uriBuilder.build().toString());
        
        HttpGet request = new HttpGet(uriBuilder.build());
        request.setHeader("Accept", "application/json; charset=UTF-8");
        
        semaphore.acquire();
        for (int i = 0; i < 5; i++) {
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    try {
                        return objectMapper.readValue(responseBody, Response.class);
                    } catch (JsonProcessingException e) {
                        semaphore.release();
                        return new Response(200, "请求成功", responseBody);
                    }
                } else {
                    Response.error("请求失败", response.getStatusLine().getStatusCode());
                    continue;
                }
            } catch (Exception e) {
                continue;
            }
        }
        semaphore.release();
        return new Response(500, "请求失败", null);
    }
    
    public API(String part) {
        this.serverUrl = part;
    }
    
    public API() {
    }
    
    public String getLastUrl() {
        return LAST_URL;
    }
    
    private String processParams(String... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        for (String param : params) {
            sb.append("&").append(param);
        }
        return sb.toString();
    }
}

