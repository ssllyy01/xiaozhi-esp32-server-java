package com.xiaozhi.service.impl;

import com.github.pagehelper.PageHelper;
import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.dao.MessageMapper;
import com.xiaozhi.entity.SysMessage;
import com.xiaozhi.service.SysMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 聊天记录
 *
 * @author Joey
 *
 */

@Service
public class SysMessageServiceImpl extends BaseServiceImpl implements SysMessageService {

    @Resource
    private MessageMapper messageMapper;

    /**
     * 新增聊天记录
     *
     * @param message
     * @return
     */
    @Override
    @Transactional(transactionManager = "transactionManager")
    public int add(SysMessage message) {
        return messageMapper.add(message);
    }

    /**
     * 查询聊天记录
     *
     * @param message
     * @return
     */
    @Override
    public List<SysMessage> query(SysMessage message, PageFilter pageFilter) {
        if(pageFilter != null){
            PageHelper.startPage(pageFilter.getStart(), pageFilter.getLimit());
        }
        return messageMapper.query(message);
    }

    /**
     * 删除记忆
     * 
     * @param message
     * @return
     */
    @Override
    @Transactional(transactionManager = "transactionManager")
    public int delete(SysMessage message) {
        return messageMapper.delete(message);
    }

}