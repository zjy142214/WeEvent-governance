package com.webank.weevent.governance.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.client.validation.Assertion;

/**
 * cross filter
 * before SingleSignOutFilter
 * @since 2018/12/18
 */
public class CORSFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            httpServletResponse.addHeader("Access-Control-Allow-Origin", "http://localhost:8080");
            httpServletResponse.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE");
            httpServletResponse.addHeader("Access-Control-Allow-Credentials", "true");
            httpServletResponse.addHeader("Access-Control-Allow-Headers", "X-Requested-With,Content-Type");
            httpServletResponse.addHeader("Access-Control-Expose-Headers", "userName");
            httpServletResponse.addHeader("x-frame-options","SAMEORIGIN");

           
            //add user header
            if(httpServletRequest.getSession().getAttribute("_const_cas_assertion_" ) !=null ) {
                Object object=httpServletRequest.getSession().getAttribute("_const_cas_assertion_" );
                Assertion assertion = (  Assertion) object;
                String username = assertion.getPrincipal().getName();
                httpServletResponse.addHeader("userName", username);
                
            }
            
            if (httpServletRequest.getMethod().equals("OPTIONS")) {
                httpServletResponse.addHeader("Access-Control-Max-Age", Integer.toString(3600));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}