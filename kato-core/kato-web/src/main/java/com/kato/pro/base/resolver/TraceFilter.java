package com.kato.pro.base.resolver;


import cn.hutool.core.util.IdUtil;
import com.kato.pro.common.constant.BaseConstant;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;


@Slf4j
@Order(0)
@WebFilter(filterName = "traceIdFilter", urlPatterns = "/*")
public class TraceFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        MDC.put(BaseConstant.TRACE_ID, IdUtil.fastSimpleUUID());
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        MDC.clear();
    }
}
