package com.kato.pro.sensitive.controller;

import com.kato.pro.base.entity.Result;
import com.kato.pro.sensitive.dto.SensitiveCheckRequest;
import com.kato.pro.sensitive.dto.SensitiveCheckResult;
import com.kato.pro.sensitive.service.ISensitiveCheckService;
import com.kato.pro.sensitive.service.ISensitiveWordHitLogService;
import com.kato.pro.sensitive.service.SensitiveSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 敏感词检测控制器
 */
@Slf4j
@RestController
@RequestMapping("/sensitive/v1")
public class SensitiveCheckController {

    @Resource
    private ISensitiveCheckService sensitiveCheckService;

    @Resource
    private SensitiveSanitizer sensitiveSanitizer;

    @Resource
    private ISensitiveWordHitLogService hitLogService;

    /**
     * 检测文本敏感词
     * wordType: EXACT-精确, REGEX-正则, ALL-全部
     * sanitize: 是否返回脱敏文本
     */
    @PostMapping("/check")
    public Result<SensitiveCheckResult> check(@RequestBody SensitiveCheckRequest request) {
        if (request.getText() == null || request.getText().isEmpty()) {
            return Result.build(1, "文本不能为空");
        }

        long startTime = System.currentTimeMillis();

        // 根据wordType选择检测方式
        String wordType = request.getWordType();
        SensitiveCheckResult result;

        if ("EXACT".equalsIgnoreCase(wordType)) {
            result = sensitiveCheckService.checkExact(request.getText());
        } else if ("REGEX".equalsIgnoreCase(wordType)) {
            result = sensitiveCheckService.check(request.getText(), false);
        } else {
            // ALL 或未指定，使用完整检测
            result = sensitiveCheckService.check(request.getText(), request.isFuzzyMatch());
        }

        // 如果需要脱敏
        if (request.isSanitize() && result.isHasSensitive()) {
            char maskChar = request.getMaskChar();
            String sanitizedText = sensitiveSanitizer.sanitize(request.getText(), result.getMatches(), maskChar);
            result.setSanitizedText(sanitizedText);
        }

        // 异步记录命中日志
        hitLogService.recordHit(request, result);

        log.info("敏感词检测完成，hasSensitive={}, matchCount={}, cost={}ms",
                result.isHasSensitive(), result.getMatchCount(), System.currentTimeMillis() - startTime);

        return Result.build(result);
    }

    /**
     * 精确匹配检测
     */
    @PostMapping("/check/exact")
    public Result<SensitiveCheckResult> checkExact(@RequestBody SensitiveCheckRequest request) {
        if (request.getText() == null || request.getText().isEmpty()) {
            return Result.build(1, "文本不能为空");
        }
        SensitiveCheckResult result = sensitiveCheckService.checkExact(request.getText());
        return Result.build(result);
    }

    /**
     * 模糊匹配检测
     */
    @PostMapping("/check/fuzzy")
    public Result<SensitiveCheckResult> checkFuzzy(@RequestBody SensitiveCheckRequest request) {
        if (request.getText() == null || request.getText().isEmpty()) {
            return Result.build(1, "文本不能为空");
        }
        SensitiveCheckResult result = sensitiveCheckService.checkFuzzy(request.getText());
        return Result.build(result);
    }

    /**
     * 按等级检测（只返回大于等于指定等级的敏感词）
     * level: URGENT/MEDIUM/NORMAL
     */
    @GetMapping("/level/{level}/check")
    public Result<SensitiveCheckResult> checkByLevel(@PathVariable String level, @RequestParam String text) {
        if (text == null || text.isEmpty()) {
            return Result.build(1, "文本不能为空");
        }
        SensitiveCheckResult result = sensitiveCheckService.checkByLevel(text, level);
        return Result.build(result);
    }
}
