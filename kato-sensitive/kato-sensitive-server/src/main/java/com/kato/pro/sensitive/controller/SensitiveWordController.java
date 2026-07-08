package com.kato.pro.sensitive.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.base.entity.Result;
import com.kato.pro.common.entity.LoginUser;
import com.kato.pro.common.resolver.LoginUserContextHolder;
import com.kato.pro.common.utils.AddrUtil;
import com.kato.pro.sensitive.dto.SensitiveWordDTO;
import com.kato.pro.sensitive.entity.SensitiveOperationLog;
import com.kato.pro.sensitive.entity.SensitiveWord;
import com.kato.pro.sensitive.listener.SensitiveWordImportListener;
import com.kato.pro.sensitive.service.ISensitiveOperationLogService;
import com.kato.pro.sensitive.service.ISensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 敏感词管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/sensitive/v1/word")
public class SensitiveWordController {

    @Resource
    private ISensitiveWordService sensitiveWordService;

    @Resource
    private ISensitiveOperationLogService operationLogService;

    /**
     * 分页查询敏感词列表
     */
    @GetMapping("/page")
    public Result<IPage<SensitiveWord>> pageSensitiveWords(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            SensitiveWord sensitiveWord) {
        Page<SensitiveWord> page = new Page<>(current, size);
        IPage<SensitiveWord> result = sensitiveWordService.pageSensitiveWords(page, sensitiveWord);
        return Result.build(result);
    }

    /**
     * 根据ID查询敏感词
     */
    @GetMapping("/{id}")
    public Result<SensitiveWord> getById(@PathVariable Long id) {
        SensitiveWord sensitiveWord = sensitiveWordService.getById(id);
        return Result.build(sensitiveWord);
    }

    /**
     * 新增敏感词
     */
    @PostMapping
    public Result<Boolean> save(@RequestBody SensitiveWord sensitiveWord) {
        boolean result = sensitiveWordService.save(sensitiveWord);
        return Result.build(result);
    }

    /**
     * 更新敏感词
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody SensitiveWord sensitiveWord) {
        boolean result = sensitiveWordService.updateById(sensitiveWord);
        return Result.build(result);
    }

    /**
     * 删除敏感词
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> removeById(@PathVariable Long id) {
        boolean result = sensitiveWordService.removeById(id);
        return Result.build(result);
    }

    /**
     * 手动触发Kafka通知
     */
    @PostMapping("/notify")
    public Result<String> sendNotice() {
        sensitiveWordService.sendKafkaNotice();
        return Result.build("通知发送成功");
    }

    /**
     * 批量导入敏感词(Excel)
     */
    @PostMapping("/import")
    public Result<String> importWords(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.build(1, "请上传文件");
        }
        String originalFilename = file.getOriginalFilename();
        try {
            SensitiveWordImportListener listener = new SensitiveWordImportListener(
                    sensitiveWordList -> sensitiveWordService.batchSave(sensitiveWordList)
            );
            EasyExcel.read(file.getInputStream(), SensitiveWordDTO.class, listener).sheet().doRead();
            // 追加一条 IMPORT 汇总日志（逐条 CREATE 日志由 batchSave 内部写入）
            operationLogService.saveLog("IMPORT", null,
                    "Excel导入: " + originalFilename,
                    currentOperatorId(), currentOperatorIp(), null, originalFilename);
            return Result.build("导入成功");
        } catch (IOException e) {
            log.error("导入敏感词失败", e);
            return Result.build(1, "导入失败: " + e.getMessage());
        }
    }

    /**
     * 条件导出敏感词(Excel)
     */
    @GetMapping("/export")
    public void exportWords(SensitiveWord sensitiveWord, HttpServletResponse response) {
        try {
            List<SensitiveWord> wordList = sensitiveWordService.listWords(sensitiveWord);
            List<SensitiveWordDTO> dtoList = wordList.stream()
                    .map(this::convertToDTO)
                    .toList();

            // 导出前记一条 EXPORT 日志（afterData 放查询条件+命中条数，便于审计）
            // 用 HashMap 而非 Map.of：sensitiveWord 可能为 null，Map.of 不接受 null key
            java.util.Map<String, Object> exportAfter = new java.util.HashMap<>();
            exportAfter.put("condition", sensitiveWord);
            exportAfter.put("count", dtoList.size());
            operationLogService.saveLog("EXPORT", null, "Excel导出",
                    currentOperatorId(), currentOperatorIp(), null, exportAfter);

            String fileName = URLEncoder.encode("敏感词导出", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            try (OutputStream outputStream = response.getOutputStream()) {
                EasyExcel.write(outputStream, SensitiveWordDTO.class)
                        .sheet("敏感词列表")
                        .doWrite(dtoList);
            }
        } catch (IOException e) {
            log.error("导出敏感词失败", e);
        }
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        try {
            String fileName = URLEncoder.encode("敏感词导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            try (OutputStream outputStream = response.getOutputStream()) {
                EasyExcel.write(outputStream, SensitiveWordDTO.class)
                        .sheet("导入模板")
                        .doWrite(List.of(new SensitiveWordDTO()));
            }
        } catch (IOException e) {
            log.error("下载模板失败", e);
        }
    }

    /**
     * 分页查询操作日志
     */
    @GetMapping("/log/page")
    public Result<IPage<SensitiveOperationLog>> pageLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            SensitiveOperationLog query) {
        Page<SensitiveOperationLog> page = new Page<>(current, size);
        IPage<SensitiveOperationLog> result = operationLogService.pageLogs(page, query);
        return Result.build(result);
    }

    private SensitiveWordDTO convertToDTO(SensitiveWord word) {
        SensitiveWordDTO dto = new SensitiveWordDTO();
        dto.setWord(word.getWord());
        dto.setLevel(word.getLevel());
        dto.setCategory(word.getCategory());
        dto.setStatus(word.getStatus());
        dto.setRemark(word.getRemark());
        return dto;
    }

    /**
     * 从线程上下文取当前操作人ID。userId 为 Integer 自增，解析失败需 log warn
     */
    private Integer currentOperatorId() {
        LoginUser loginUser = LoginUserContextHolder.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            return null;
        }
        try {
            return Integer.parseInt(loginUser.getUserId());
        } catch (NumberFormatException e) {
            log.warn("LoginUser.userId 非数字，userId={}", loginUser.getUserId());
            return null;
        }
    }

    /**
     * 从当前 HTTP 请求里取客户端 IP（带反代头兼容）
     */
    private String currentOperatorIp() {
        try {
            return AddrUtil.getRemoteAddr(getCurrentRequest());
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("当前线程非Web请求上下文，无法获取客户端IP");
        }
        return attributes.getRequest();
    }
}
