package com.sdu.rabbix.common.interceptor;

import cn.hutool.extra.servlet.ServletUtil;
import com.sdu.rabbix.common.domain.dto.RequestInfo;
import com.sdu.rabbix.common.utils.UserContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class IPInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RequestInfo requestInfo = UserContextHolder.get();
        if (null == requestInfo) requestInfo = new RequestInfo();
        requestInfo.setIp(ServletUtil.getClientIP(request));
        UserContextHolder.set(requestInfo);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.remove();
    }
}
