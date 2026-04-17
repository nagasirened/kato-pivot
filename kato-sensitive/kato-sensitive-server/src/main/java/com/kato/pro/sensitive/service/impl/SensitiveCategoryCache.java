package com.kato.pro.sensitive.service.impl;

import com.kato.pro.sensitive.entity.SensitiveWord;
import com.kato.pro.sensitive.entity.constant.SensitiveCategory;
import com.kato.pro.sensitive.mapper.SensitiveWordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 敏感词分类本地缓存
 * 启动时全量加载，之后每小时定时刷新
 */
@Slf4j
@Component
public class SensitiveCategoryCache {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    /**
     * 按分类分组的敏感词缓存
     */
    private volatile Map<String, List<SensitiveWord>> categoryWordMap = new ConcurrentHashMap<>();

    /**
     * 全量敏感词缓存
     */
    private volatile List<SensitiveWord> allWords = Collections.emptyList();

    /**
     * 缓存版本号
     */
    private volatile long version = 0;

    @PostConstruct
    public void init() {
        refreshCache();
        log.info("敏感词分类缓存初始化完成，当前版本={}", version);
    }

    /**
     * 每小时刷新一次缓存
     */
    @Scheduled(fixedRate = 3600000)
    public void refreshCache() {
        try {
            List<SensitiveWord> words = sensitiveWordMapper.selectList(null);
            Map<String, List<SensitiveWord>> newMap = words.stream()
                    .filter(w -> w.getCategory() != null)
                    .collect(Collectors.groupingBy(SensitiveWord::getCategory));

            synchronized (this) {
                categoryWordMap = new ConcurrentHashMap<>(newMap);
                allWords = words;
                version++;
            }
            log.info("敏感词分类缓存刷新完成，敏感词总数={}", words.size());
        } catch (Exception e) {
            log.error("刷新敏感词分类缓存失败", e);
        }
    }

    /**
     * 获取指定分类的敏感词列表
     */
    public List<SensitiveWord> getWordsByCategory(String category) {
        return categoryWordMap.getOrDefault(category, Collections.emptyList());
    }

    /**
     * 获取所有敏感词
     */
    public List<SensitiveWord> getAllWords() {
        return allWords;
    }

    /**
     * 获取当前缓存版本
     */
    public long getVersion() {
        return version;
    }

    /**
     * 获取所有分类
     */
    public List<SensitiveCategory> getAllCategories() {
        return Arrays.stream(SensitiveCategory.getAllCategories()).collect(Collectors.toList());
    }
}
