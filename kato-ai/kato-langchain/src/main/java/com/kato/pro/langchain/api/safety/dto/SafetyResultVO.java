package com.kato.pro.langchain.api.safety.dto;

import com.kato.pro.langchain.domain.safety.SafetyResult;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 内容安全检测结果 VO。
 *
 *   - passed        : 是否通过
 *   - hitWords      : 命中关键词（脱敏展示用，前端不强制渲染）
 *   - sanitizedText : 脱敏文本（如有）
 *   - fallback      : 是否为降级结果
 *   - details       : 其它元数据
 */
@Value
@Builder
@Schema(description = "安全检测结果视图")
public class SafetyResultVO {
    boolean passed;
    List<String> hitWords;
    String sanitizedText;
    boolean fallback;
    Map<String, Object> details;

    public static SafetyResultVO from(SafetyResult r) {
        if (r == null) return null;
        return SafetyResultVO.builder()
                .passed(r.isPassed())
                .hitWords(r.getHitWords())
                .sanitizedText(r.getSanitizedText())
                .fallback(r.isFallback())
                .details(r.getDetails())
                .build();
    }
}
