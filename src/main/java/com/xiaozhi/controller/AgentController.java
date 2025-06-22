package com.xiaozhi.controller;

import com.xiaozhi.common.web.AjaxResult;
import com.xiaozhi.entity.SysAgent;
import com.xiaozhi.service.SysAgentService;
import com.xiaozhi.utils.CmsUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体管理
 * 
 * @author Joey
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController extends BaseController {
    @Resource
    private SysAgentService agentService;

    /**
     * 查询智能体列表
     * 
     * @param agent    查询条件
     * @return 智能体列表
     */
    @GetMapping("/query")
    @ResponseBody
    public AjaxResult query(SysAgent agent) {
        try {
            List<SysAgent> sysAgents = agentService.query(agent);
            Map<String, Object> data = new HashMap<>();
            data.put("list", sysAgents);
            data.put("total", sysAgents.size());
            return AjaxResult.success(data);
        }catch (Exception e){
            logger.error("查询智能体列表失败", e);
            return AjaxResult.error("查询智能体列表失败");
        }
    }

    /**
     * 添加智能体
     * 
     * @param agent    智能体信息
     * @return 添加结果
     */
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody SysAgent agent) {
        try {

            agent.setUserId(CmsUtils.getUserId());
            agentService.add(agent);
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResult.error("添加智能体失败");
        }
    }

    /**
     * 更新智能体
     * 
     * @param agent 智能体信息
     * @return 更新结果
     */
    @PostMapping("/update")
    @ResponseBody
    public AjaxResult update(@RequestBody SysAgent agent) {
        try {
            agentService.update(agent);
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResult.error("更新智能体失败");
        }
    }

    /**
     * 删除智能体
     * 
     * @param agent 智能体信息
     * @return 删除结果
     */
    @PostMapping("/delete")
    @ResponseBody
    public AjaxResult delete(@RequestBody SysAgent agent) {
        try {
            agentService.delete(agent.getAgentId());
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResult.error("删除智能体失败");
        }
    }
}