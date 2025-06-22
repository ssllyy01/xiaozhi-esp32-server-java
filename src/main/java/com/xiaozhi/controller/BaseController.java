package com.xiaozhi.controller;

import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.service.impl.BaseServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @description: 基础控制器
 *
 * @author Joey
 *
 */
public class BaseController {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 最大分页数量
     */
    public static final int MAX_PAGE_SIZE = 1000;

    protected PageFilter initPageFilter(HttpServletRequest request) {
        String start = request.getParameter("start");
        String limit = request.getParameter("limit");
        if (!StringUtils.hasText(start) && !StringUtils.hasText(limit)) {
            return null;
        }
        PageFilter pageFilter = new PageFilter();
        if(StringUtils.hasText(start)){
            pageFilter.setStart(Integer.parseInt(start));
        }
        if(StringUtils.hasText(limit)){
            int limitValue = Math.min(Integer.parseInt(limit), MAX_PAGE_SIZE);
            pageFilter.setLimit(limitValue);
        }
        RequestContextHolder.currentRequestAttributes().setAttribute(BaseServiceImpl.PAGE_ATTRIBUTE_KEY, pageFilter, RequestAttributes.SCOPE_REQUEST);
        return pageFilter;
    }
}
