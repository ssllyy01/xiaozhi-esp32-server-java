package com.xiaozhi.controller;

import com.github.pagehelper.PageInfo;
import com.xiaozhi.common.web.AjaxResult;
import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.dialogue.tts.factory.TtsServiceFactory;
import com.xiaozhi.entity.SysConfig;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysConfigService;
import com.xiaozhi.service.SysRoleService;
import com.xiaozhi.utils.CmsUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理
 * 
 * @author Joey
 * 
 */

@RestController
@RequestMapping("/api/role")
public class RoleController extends BaseController {

    @Resource
    private SysRoleService roleService;

    @Resource
    private TtsServiceFactory ttsService;

    @Resource
    private SysConfigService configService;

    /**
     * 角色查询
     * 
     * @param role
     * @return roleList
     */
    @GetMapping("/query")
    @ResponseBody
    public AjaxResult query(SysRole role, HttpServletRequest request) {
        try {
            PageFilter pageFilter = initPageFilter(request);
            role.setUserId(CmsUtils.getUserId());
            List<SysRole> roleList = roleService.query(role, pageFilter);
            AjaxResult result = AjaxResult.success();
            result.put("data", new PageInfo<>(roleList));
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }

    /**
     * 角色信息更新
     * 
     * @param role
     * @return
     */
    @PostMapping("/update")
    @ResponseBody
    public AjaxResult update(SysRole role) {
        try {
            role.setUserId(CmsUtils.getUserId());
            roleService.update(role);
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }

    /**
     * 添加角色
     * 
     * @param role
     */
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(SysRole role) {
        try {
            role.setUserId(CmsUtils.getUserId());
            roleService.add(role);
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }

    @GetMapping("/testVoice")
    @ResponseBody
    public AjaxResult testAudio(String message, String provider, Integer ttsId, String voiceName) {
        SysConfig config = null;
        try {
            if (!provider.equals("edge")) {
                config = configService.selectConfigById(ttsId);
            }
            String audioFilePath = ttsService.getTtsService(config, voiceName).textToSpeech(message);
            AjaxResult result = AjaxResult.success();
            result.put("data", audioFilePath);
            return result;
        } catch (IndexOutOfBoundsException e) {
            return AjaxResult.error("请先到语音合成配置页面配置对应Key");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return AjaxResult.error();
        }
    }
}