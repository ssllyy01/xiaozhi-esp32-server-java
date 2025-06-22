package com.xiaozhi.controller;

import com.xiaozhi.common.web.AjaxResult;
import com.xiaozhi.utils.FileUploadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传控制器
 * 
 * @author Joey
 */
@RestController
@RequestMapping("/api/file")
public class FileUploadController {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Value("${xiaozhi.upload-path:uploads}")
    private String uploadPath;

    /**
     * 通用文件上传方法
     * 
     * @param file 上传的文件
     * @param type 文件类型（可选，用于分类存储）
     * @return 文件访问URL
     */
    @PostMapping("/upload")
    @ResponseBody
    public AjaxResult uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false, defaultValue = "common") String type) {

        // 构建文件存储路径，按日期和类型分类
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = type + "/" + datePath;

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString().replaceAll("-", "") + extension;

        // 使用FileUploadUtils进行智能上传（根据配置自动选择上传到本地或腾讯云COS）
        try {
            String fileUrl = FileUploadUtils.smartUpload(uploadPath, relativePath, fileName, file);
            logger.info("文件上传成功: {}", fileUrl);

            // 判断是否是COS URL
            boolean isCosUrl = fileUrl.startsWith("https://") && fileUrl.contains(".cos.");

            AjaxResult result = AjaxResult.success("上传成功");
            result.put("url", fileUrl);
            result.put("fileName", originalFilename);
            result.put("newFileName", fileName);

            // 如果是本地URL，需要调整格式
            if (!isCosUrl) {
                // 将本地路径转换为访问URL格式
                String accessUrl = relativePath + "/" + fileName;
                result.put("url", accessUrl);
            }

            return result;
        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            return AjaxResult.error("文件上传失败: " + e.getMessage());
        }
    }
}