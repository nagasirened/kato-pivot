package com.kato.pro.sensitive.service.impl;

import com.kato.pro.sensitive.dto.SensitiveCheckRequest;
import com.kato.pro.sensitive.dto.SensitiveCheckResult;
import com.kato.pro.sensitive.dto.SensitiveCheckResult.MatchItem;
import com.kato.pro.sensitive.entity.SensitiveWordHitLog;
import com.kato.pro.sensitive.mapper.SensitiveWordHitLogMapper;
import com.kato.pro.sensitive.service.ISensitiveWordHitLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 敏感词命中日志服务实现
 */
@Slf4j
@Service
public class SensitiveWordHitLogServiceImpl implements ISensitiveWordHitLogService {

    @Resource
    private SensitiveWordHitLogMapper hitLogMapper;

    @Override
    @Async
    public void recordHit(SensitiveCheckRequest request, SensitiveCheckResult result) {
        try {
            doRecordHit(request, result);
        } catch (Exception e) {
            log.warn("记录敏感词命中日志失败: {}", e.getMessage());
        }
    }

    @Override
    public void recordHitSync(SensitiveCheckRequest request, SensitiveCheckResult result) {
        try {
            doRecordHit(request, result);
        } catch (Exception e) {
            log.warn("记录敏感词命中日志失败: {}", e.getMessage());
        }
    }

    private void doRecordHit(SensitiveCheckRequest request, SensitiveCheckResult result) {
        SensitiveWordHitLog hitLog = new SensitiveWordHitLog();
        hitLog.setTextLength(request.getText() != null ? request.getText().length() : 0);
        hitLog.setHitCount(result.getMatchCount());

        // 提取等级列表
        if (result.getMatches() != null && !result.getMatches().isEmpty()) {
            String levels = result.getMatches().stream()
                    .map(MatchItem::getLevel)
                    .distinct()
                    .collect(Collectors.joining(","));
            hitLog.setHitLevels(levels);

            // 提取敏感词列表
            String words = result.getMatches().stream()
                    .map(MatchItem::getWord)
                    .distinct()
                    .collect(Collectors.joining(","));
            hitLog.setHitWords(words);
        }

        // 检测类型
        String checkType = determineCheckType(request, result);
        hitLog.setCheckType(checkType);

        hitLog.setCheckTime(LocalDateTime.now());

        hitLogMapper.insert(hitLog);
    }

    private String determineCheckType(SensitiveCheckRequest request, SensitiveCheckResult result) {
        if (result.getMatches() == null || result.getMatches().isEmpty()) {
            return "NONE";
        }

        boolean hasExact = result.getMatches().stream()
                .anyMatch(m -> "EXACT".equals(m.getMatchType()));
        boolean hasRegex = result.getMatches().stream()
                .anyMatch(m -> "REGEX".equals(m.getMatchType()));
        boolean hasFuzzy = result.getMatches().stream()
                .anyMatch(m -> "FUZZY".equals(m.getMatchType()));

        if (hasExact && hasRegex && hasFuzzy) {
            return "ALL";
        } else if (hasExact && hasRegex) {
            return "EXACT,REGEX";
        } else if (hasExact && hasFuzzy) {
            return "EXACT,FUZZY";
        } else if (hasRegex && hasFuzzy) {
            return "REGEX,FUZZY";
        } else if (hasExact) {
            return "EXACT";
        } else if (hasRegex) {
            return "REGEX";
        } else {
            return "FUZZY";
        }
    }
}
