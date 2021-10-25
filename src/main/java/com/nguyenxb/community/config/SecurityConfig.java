package com.nguyenxb.community.config;

import com.nguyenxb.community.util.CommunityConstant;
import com.nguyenxb.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 所有resources目录下的资源都能访问
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeRequests()
                // 登录才能访问的功能
                .antMatchers(
                        "/user/setting", // 用户设置
                        "/user/upload", // 用户上传头像
                        "/discuss/add", // 用户发布帖子
                        "/comment/add/**", // 评论
                        "/letter/**", // 私信
                        "/notice/**", // 查看通知
                        "/like", // 点赞
                        "/follow", // 关注
                        "/unfollow" // 取消关注
                        )
                // 设置访问的用户权限, 用户,管理员,版主都能访问
                .hasAnyAuthority(
                        AUTHORITY_USER, // 普通用户
                        AUTHORITY_ADMIN, // 管理员
                        AUTHORITY_MODERATOR // 版主
                    )
                // 版主能进行加精,置顶
                .antMatchers(
                        "/discuss/top", // 置顶
                        "/discuss/wonderful" // 加精
                )
                .hasAnyAuthority(
                    AUTHORITY_MODERATOR
                )
                // 管理员能删除帖子
                .antMatchers(
                        "/discuss/delete",
                        "/data/**"
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN
                )
                // 其他请求都能访问
                .anyRequest().permitAll()
                // 关闭csrf检查
                .and().csrf().disable();

        // 当权限不够的时候,怎么处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        // 没有登录时处理
                        String header = request.getHeader("x-requested-with");

                        if ("XMLHttpRequest".equals(header)){
                            // 如果是异步请求,返回json消息
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你还没有登录!"));
                        }else {
                            // 如果时普通请求,跳转会登录页面
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        // 权限不足的时候处理
                        String header = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(header)){
                            // 如果是异步请求,返回json消息
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你没有访问此功能的权限!"));
                        }else {
                            // 如果时普通请求,跳转会登录页面
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });


        // security 底层默认会拦截/logout 请求,进行退出处理
        // 覆盖它默认的逻辑,才能执行

        http.logout().logoutUrl("/securitylogout");


    }
}
