package com.webank.weevent.governance.filter;

import java.io.IOException;
import java.net.InetAddress;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * LoginHelperFilter
 * after Cas20ProxyReceivingTicketValidationFilter
 * @since 2018/12/18
 */
public class LoginHelperFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        
        String originUrl = httpServletRequest.getParameter("originUrl");
        if (originUrl != null) {
            InetAddress ia=InetAddress.getLocalHost();
            String localip=ia.getHostAddress();
            //resolve open redirect risk
            if(originUrl.contains("?")) {
                String subUrl = originUrl.substring(0, originUrl.indexOf("?"));
                if(subUrl.contains(localip) || subUrl.contains("127.0.0.1")) {
                    httpServletResponse.sendRedirect(originUrl);
                }else {
                    return;
                }
            }else {
                if(originUrl.contains(localip) || originUrl.contains("127.0.0.1")) {
                    httpServletResponse.sendRedirect(originUrl);
                }else {
                    return;
                }
            }
        } else if (httpServletRequest.getRequestURI().endsWith("umLoginCheck")) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("um login ok");
            response.flushBuffer();
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }
}