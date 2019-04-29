package com.webank.weevent.governance.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.webank.weevent.governance.entity.Broker;
import com.webank.weevent.governance.service.BrokerService;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HttpForwardFilter implements Filter{
	
	@Autowired
    BrokerService brokerService;
	
	@Autowired
	CloseableHttpClient httpClient;
	
	@Value("${weevent.url}")
    private String url;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		String idStr = request.getParameter("id");
		String originUrl = req.getRequestURI();
		String subStrUrl = originUrl.substring(originUrl.indexOf("/weevent/") + 8);
		
		String newUrl = "";
		if(!StringUtils.isBlank(idStr)) {
			Integer id = Integer.parseInt(idStr);
			Broker broker = brokerService.getBroker(id);
			String brokerUrl = broker.getUrl();
			newUrl = brokerUrl + subStrUrl;
		}
		newUrl = url + subStrUrl;
		
		CloseableHttpResponse closeResponse = null;
		if(req.getMethod().equals("GET")) {
			HttpGet get = getMethod(newUrl,req);    //创建get请求
			closeResponse = httpClient.execute(get);   //执行get请求
		}else {
			HttpPost postMethod = postMethod(newUrl, req);
			closeResponse = httpClient.execute(postMethod);
		}
		String mes = EntityUtils.toString(closeResponse.getEntity()); 
		log.info("response: " +mes);
		Header encode = closeResponse.getFirstHeader("Content-Type");  //请求头，要返回content-type，以便前台知道如何处理
        res.setHeader(encode.getName(), encode.getValue());
        ServletOutputStream out = response.getOutputStream();
	    out.write(mes.getBytes());
		return ;
//	    chain.doFilter(request, response);
	}
	
	/**
	 * 返回get方法 填充前台传送来的参数
	 *
	 * @param uri 要请求的接口地址
	 * @param request 前台请求过来后 controller层请求对象
	 * @author piper
	 * @data 2018/7/3 11:19
	 */
	HttpGet getMethod(String uri, HttpServletRequest request) {
	    try {
	        URIBuilder builder = new URIBuilder(uri);
	        Enumeration<String> enumeration = request.getParameterNames();
	        //将前台的参数放到我的请求里面
	        while (enumeration.hasMoreElements()) {
	            String nex = enumeration.nextElement();
	            builder.setParameter(nex, request.getParameter(nex));
	        }
	        return new HttpGet(builder.build());
	    } catch (URISyntaxException e) {
	        e.printStackTrace();
	    }
	    return null;
	}
	
	/**
	* 返回post方法
	 *
	 * @param uri 要请求的地址
	 * @param request 前台请求对象
	 * @author piper
	 * @data 2018/7/3 11:19
	 */
	HttpPost postMethod(String uri, HttpServletRequest request) {
	    StringEntity entity = null;
	    if (request.getContentType().contains("json")) {
	        entity = jsonData(request);  //填充json数据
	    } else {
	        entity = formData(request);  //填充form数据
	    }
	    HttpPost httpPost = new HttpPost(uri);
	    httpPost.setHeader("Content-Type", request.getHeader("Content-Type"));
	    httpPost.setEntity(entity);
	    return httpPost;
	}
	
	/**
	 * 处理post请求 form数据 填充form数据
	 *
	 * @param request 前台请求
	 * @author piper
	 * @data 2018/7/17 18:05
	 */
	public UrlEncodedFormEntity formData(HttpServletRequest request) {
	    UrlEncodedFormEntity urlEncodedFormEntity = null;
	    try {
	        List<NameValuePair> pairs = new ArrayList<>();  //存储参数
	        Enumeration<String> params = request.getParameterNames();  //获取前台传来的参数
	        while (params.hasMoreElements()) {
	            String name = params.nextElement();
	            pairs.add(new BasicNameValuePair(name, request.getParameter(name)));
	        }
	        //根据参数创建参数体，以便放到post方法中
	        urlEncodedFormEntity = new UrlEncodedFormEntity(pairs, request.getCharacterEncoding());
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	    }
	    return urlEncodedFormEntity;
	}
	
	/**
	 * 处理post请求 json数据
	 *
	 * @param request 前台请求
	 * @author piper
	 * @data 2018/7/17 18:05
	 */
	public StringEntity jsonData(HttpServletRequest request) {
	    InputStreamReader is = null;
	    try {
	        is = new InputStreamReader(request.getInputStream(), request.getCharacterEncoding());
	        BufferedReader reader = new BufferedReader(is);
	        //将json数据放到String中
	        StringBuilder sb = new StringBuilder();
	        String line = null;
	        while ((line = reader.readLine()) != null) {
	            sb.append(line);
	        }
	        //根据json数据创建请求体
	        return new StringEntity(sb.toString(), request.getCharacterEncoding()); 
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            is.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    return null;
	}
	
	/**
	 * 处理返回文件
	 * 
	 * @param response 前台页面的响应
	 * @param closeableHttpResponse  httpclient请求过后返回数据
	 * @date 2018/04/29 
	 */
	public void fileHandle(HttpServletResponse response, CloseableHttpResponse closeableHttpResponse) {
	    ServletOutputStream out = null;
	    try {
	        Header encode = closeableHttpResponse.getFirstHeader("Content-Type");  //请求头，要返回content-type，以便前台知道如何处理
	        response.setHeader(encode.getName(), encode.getValue());
	        HttpEntity entity = closeableHttpResponse.getEntity();  //取出返回体
	        
	        out = response.getOutputStream();  //得到给前台的响应流
	        entity.writeTo(out);  //将返回体通过响应流写到前台
	        out.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            if (out != null) {
	                out.close();
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	}

}
