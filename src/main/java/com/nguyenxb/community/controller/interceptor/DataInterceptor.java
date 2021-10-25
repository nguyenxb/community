package com.nguyenxb.community.controller.interceptor;

import com.nguyenxb.community.entity.User;
import com.nguyenxb.community.service.DataService;
import com.nguyenxb.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataInterceptor implements HandlerInterceptor {

    @Autowired
    private DataService dataService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 统计UV 访问用户
        String ip = request.getRemoteHost();
        dataService.recordUV(ip);

        // 统计日活跃用户
        User user = hostHolder.getUser();
        if (user != null){
            dataService.recordDAV(user.getId());
        }
        return true;
    }
}
