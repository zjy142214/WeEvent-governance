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
	
	@Autowired
	CloseableHttpClient httpsClient;
	
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
			HttpGet get = getMethod(newUrl,req);    
			if(newUrl.startsWith("https")) {
				closeResponse = httpsClient.execute(get);
			}else {
				closeResponse = httpClient.execute(get);   
			}
			
		}else {
			HttpPost postMethod = postMethod(newUrl, req);
			if(newUrl.startsWith("https")) {
				closeResponse = httpsClient.execute(postMethod);
			}else {
				closeResponse = httpClient.execute(postMethod);   
			}
		}
		String mes = EntityUtils.toString(closeResponse.getEntity()); 
		log.info("response: " +mes);
		Header encode = closeResponse.getFirstHeader("Content-Type");  
        res.setHeader(encode.getName(), encode.getValue());
        ServletOutputStream out = response.getOutputStream();
	    out.write(mes.getBytes());
		return ;
//	    chain.doFilter(request, response);
	}
	
	/**
	 * return HttpGet 
	 *
	 */
	HttpGet getMethod(String uri, HttpServletRequest request) {
	    try {
	        URIBuilder builder = new URIBuilder(uri);
	        Enumeration<String> enumeration = request.getParameterNames();
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
	
	HttpPost postMethod(String uri, HttpServletRequest request) {
	    StringEntity entity = null;
	    if (request.getContentType().contains("json")) {
	        entity = jsonData(request);  
	    } else {
	        entity = formData(request);  
	    }
	    HttpPost httpPost = new HttpPost(uri);
	    httpPost.setHeader("Content-Type", request.getHeader("Content-Type"));
	    httpPost.setEntity(entity);
	    return httpPost;
	}
	
	public UrlEncodedFormEntity formData(HttpServletRequest request) {
	    UrlEncodedFormEntity urlEncodedFormEntity = null;
	    try {
	        List<NameValuePair> pairs = new ArrayList<>();  
	        Enumeration<String> params = request.getParameterNames();  
	        while (params.hasMoreElements()) {
	            String name = params.nextElement();
	            pairs.add(new BasicNameValuePair(name, request.getParameter(name)));
	        }
	       
	        urlEncodedFormEntity = new UrlEncodedFormEntity(pairs, request.getCharacterEncoding());
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	    }
	    return urlEncodedFormEntity;
	}
	
	public StringEntity jsonData(HttpServletRequest request) {
	    InputStreamReader is = null;
	    try {
	        is = new InputStreamReader(request.getInputStream(), request.getCharacterEncoding());
	        BufferedReader reader = new BufferedReader(is);
	        StringBuilder sb = new StringBuilder();
	        String line = null;
	        while ((line = reader.readLine()) != null) {
	            sb.append(line);
	        }
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
	
	public void fileHandle(HttpServletResponse response, CloseableHttpResponse closeableHttpResponse) {
	    ServletOutputStream out = null;
	    try {
	        Header encode = closeableHttpResponse.getFirstHeader("Content-Type");  
	        response.setHeader(encode.getName(), encode.getValue());
	        HttpEntity entity = closeableHttpResponse.getEntity();  
	        
	        out = response.getOutputStream();  
	        entity.writeTo(out);  
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
