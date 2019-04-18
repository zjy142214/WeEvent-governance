package com.webank.weevent.governance.configuration;

import org.jasig.cas.client.authentication.AuthenticationFilter;
import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.session.SingleSignOutHttpSessionListener;
import org.jasig.cas.client.util.AssertionThreadLocalFilter;
import org.jasig.cas.client.util.HttpServletRequestWrapperFilter;
import org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.webank.weevent.governance.filter.CORSFilter;
import com.webank.weevent.governance.filter.LoginHelperFilter;
import com.webank.weevent.governance.filter.XssFilter;

@Configuration
public class WeeventConfiguration {
    
    @Value("${governance.influxdb.enabled}")
    private String enabled;
    @Value("${governance.influxdb.username}")
    private String username;
    @Value("${governance.influxdb.password}")
    private String password;
    @Value("${governance.influxdb.openurl}")
    private String openurl;
    @Value("${governance.influxdb.database}")
    private String database;
    
    @Value("${weevent.url}")
    private String weeventUrl;
     
    @Bean
    public InfluxDBConnect getInfluxDBConnect(){
        InfluxDBConnect influxDB = new InfluxDBConnect(enabled,username, password, openurl, database);
      if(enabled != null && enabled.equals("true")) {
          influxDB.influxDbBuild();
          influxDB.createRetentionPolicy();
      }
      return influxDB;
    }


    /**
     * get RestTemplate Bean
     * @param factory
     * @return
     */
    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
        if(weeventUrl.startsWith("https")) {
            return new RestTemplate(factory);
        }else {
            return new RestTemplate();
        }
    }

    @Bean
    public ClientHttpRequestFactory httpsClientRequestFactory() {
        HttpsClientRequestFactory factory = new HttpsClientRequestFactory();
        factory.setReadTimeout(5000);// ms
        factory.setConnectTimeout(15000);// ms
        return factory;
    }
    
    @Bean
    public FilterRegistrationBean<CORSFilter> crosRegist() {
        FilterRegistrationBean<CORSFilter> frBean = new FilterRegistrationBean<>();
        frBean.setFilter(new CORSFilter());
        frBean.addUrlPatterns("/*");
        frBean.setOrder(1);
        return frBean;
    }
    
    @Bean
    public FilterRegistrationBean<SingleSignOutFilter> singleSignOutRegist() {
        FilterRegistrationBean<SingleSignOutFilter> frBean = new FilterRegistrationBean<>();
        frBean.setFilter(new SingleSignOutFilter());
        frBean.addUrlPatterns("/*");
        frBean.setOrder(2);
        return frBean;
    }
    
    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegist() {
        FilterRegistrationBean<AuthenticationFilter> frBean = new FilterRegistrationBean<>();
        frBean.setFilter(new AuthenticationFilter());
        frBean.addUrlPatterns("/*");
        frBean.setOrder(3);
        return frBean;
    }
    
    @Bean
    public FilterRegistrationBean<Cas20ProxyReceivingTicketValidationFilter> cas20ProxyReceivingTicketValidationFilterRegist() {
        FilterRegistrationBean<Cas20ProxyReceivingTicketValidationFilter> frBean = new FilterRegistrationBean<>();
        frBean.setFilter(new Cas20ProxyReceivingTicketValidationFilter());
        frBean.addInitParameter("hostnameVerifier", "org.jasig.cas.client.ssl.AnyHostnameVerifier");
        frBean.addUrlPatterns("/*");
        frBean.setOrder(4);
        return frBean;
    }
    
    @Bean
    public FilterRegistrationBean<LoginHelperFilter> loginHelperFilterRegist() {
        FilterRegistrationBean<LoginHelperFilter> frBean = new FilterRegistrationBean<>();
        frBean.setFilter(new LoginHelperFilter());
        frBean.addUrlPatterns("/*");
        frBean.setOrder(5);
        return frBean;
    }
    
    @Bean
    public FilterRegistrationBean<HttpServletRequestWrapperFilter> httpServletRequestWrapperFilterRegist() {
        FilterRegistrationBean<HttpServletRequestWrapperFilter> frBean = new FilterRegistrationBean<>();
        frBean.setFilter(new HttpServletRequestWrapperFilter());
        frBean.setOrder(6);
        frBean.addUrlPatterns("/*");
        return frBean;
    }
    
    @Bean
    public FilterRegistrationBean<AssertionThreadLocalFilter> assertionThreadLocalFilterRegist() {
        FilterRegistrationBean<AssertionThreadLocalFilter> frBean = new FilterRegistrationBean<>();
        frBean.setFilter(new AssertionThreadLocalFilter());
        frBean.addUrlPatterns("/*");
        frBean.setOrder(7);
        return frBean;
    }
    
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistrationBean() {
        FilterRegistrationBean<XssFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new XssFilter());
        filterRegistrationBean.setOrder(8);
        filterRegistrationBean.setEnabled(true);
        filterRegistrationBean.addUrlPatterns("/*");
        return filterRegistrationBean;
    }

    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    public ServletListenerRegistrationBean listenerRegist() {
        ServletListenerRegistrationBean srb = new ServletListenerRegistrationBean();
        srb.setListener(new SingleSignOutHttpSessionListener());
        return srb;
    }
    
}
