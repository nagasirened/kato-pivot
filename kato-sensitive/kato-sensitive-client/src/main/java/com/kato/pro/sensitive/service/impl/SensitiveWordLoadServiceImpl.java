package com.kato.pro.sensitive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kato.pro.sensitive.algorithm.RegexMatchAlgorithm;
import com.kato.pro.sensitive.dfa.SensitiveWordDFA;
import com.kato.pro.sensitive.entity.SensitiveWord;
import com.kato.pro.sensitive.mapper.SensitiveWordMapper;
import com.kato.pro.sensitive.service.SensitiveWordLoadService;
import com.kato.pro.sensitive.trie.SensitiveWordTrie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

/**
 * 敏感词加载服务实现
 */
@Slf4j
@Service
public class SensitiveWordLoadServiceImpl implements SensitiveWordLoadService {

    @Resource
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private SensitiveWordTrie sensitiveWordTrie;

    @Autowired
    private SensitiveWordDFA sensitiveWordDFA;

    @Autowired
    private RegexMatchAlgorithm regexMatchAlgorithm;

    private volatile long version = 0;

    @PostConstruct
    @Override
    public void loadSensitiveWords() {
        try {
            LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SensitiveWord::getStatus, "ENABLE");
            List<SensitiveWord> wordList = sensitiveWordMapper.selectList(wrapper);

            // 按类型分组
            List<SensitiveWord> exactWords = new java.util.ArrayList<>();
            List<SensitiveWord> regexWords = new java.util.ArrayList<>();
            for (SensitiveWord word : wordList) {
                if ("REGEX".equalsIgnoreCase(word.getWordType())) {
                    regexWords.add(word);
                } else {
                    exactWords.add(word);
                }
            }

            // 加载到Trie树
            sensitiveWordTrie.clear();
            sensitiveWordTrie.addWords(exactWords);

            // 加载到DFA
            sensitiveWordDFA.clear();
            sensitiveWordDFA.addWords(exactWords);

            // 加载到正则匹配器
            regexMatchAlgorithm.clear();
            regexMatchAlgorithm.addWords(regexWords);

            version++;
            log.info("敏感词加载完成，共加载敏感词数量={}, EXACT={}, REGEX={}, version={}",
                    wordList.size(), exactWords.size(), regexWords.size(), version);
        } catch (Exception e) {
            log.error("加载敏感词失败", e);
        }
    }

    @Override
    public void hotReloadSensitiveWords() {
        log.info("开始热更新敏感词...");
        loadSensitiveWords();
        log.info("热更新敏感词完成, version={}", version);
    }

    @Override
    public long getVersion() {
        return version;
    }
}
