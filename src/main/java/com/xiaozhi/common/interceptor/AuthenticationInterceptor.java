package com.xiaozhi.common.interceptor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaozhi.common.web.AjaxResult;
import com.xiaozhi.entity.SysUser;
import com.xiaozhi.service.SysUserService;
import com.xiaozhi.utils.CmsUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Resource
    private SysUserService userService;

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationInterceptor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 不需要认证的路径
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/user/",
            "/api/device/ota",
            "/audio/",
            "/uploads/",
            "/avatar/",
            "/ws/");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 检查是否是公共路径
        if (isPublicPath(path)) {
            return true;
        }

        // 获取会话
        HttpSession session = request.getSession(false);
        if (session != null) {
            // 检查会话中是否有用户
            Object userObj = session.getAttribute(SysUserService.USER_SESSIONKEY);
            if (userObj != null) {
                SysUser user = (SysUser) userObj;
                // 将用户信息存储在请求属性中
                request.setAttribute(CmsUtils.USER_ATTRIBUTE_KEY, user);
                CmsUtils.setUser(request, user);
                return true;
            }
        }

        // 尝试从Cookie中获取用户名
        if (tryAuthenticateWithCookies(request, response)) {
            return true;
        }

        // 处理未授权的请求
        handleUnauthorized(request, response);
        return false;
    }

    /**
     * 尝试使用Cookie进行认证
     */
    private boolean tryAuthenticateWithCookies(HttpServletRequest request, HttpServletResponse response) {
        // 检查是否有username cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("username".equals(cookie.getName())) {
                    String username = cookie.getValue();
                    if (StringUtils.isNotBlank(username)) {
                        SysUser user = userService.selectUserByUsername(username);
                        if (user != null) {
                            // 将用户存储在会话和请求属性中
                            HttpSession session = request.getSession(true);
                            session.setAttribute(SysUserService.USER_SESSIONKEY, user);
                            request.setAttribute(CmsUtils.USER_ATTRIBUTE_KEY, user);
                            CmsUtils.setUser(request, user);
                            return true;
                        }
                    }
                    break;
                }
            }
        }
        return false;
    }

    /**
     * 处理未授权的请求
     */
    private void handleUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 检查是否是AJAX请求
        String ajaxTag = request.getHeader("Request-By");
        String head = request.getHeader("X-Requested-With");

        if ((ajaxTag != null && ajaxTag.trim().equalsIgnoreCase("Ext"))
                || (head != null && !head.equalsIgnoreCase("XMLHttpRequest"))) {
            response.addHeader("_timeout", "true");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            // 返回JSON格式的错误信息
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            AjaxResult result = AjaxResult.error(com.xiaozhi.common.web.HttpStatus.FORBIDDEN, "用户未登录");
            try {
                objectMapper.writeValue(response.getOutputStream(), result);
            } catch (Exception e) {
                logger.error("写入响应失败", e);
                throw e;
            }
        }
    }

    /**
     * 检查是否是公共路径
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}