package com.kato.pro.sensitive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.sensitive.entity.SensitiveWord;
import com.kato.pro.sensitive.mapper.SensitiveWordMapper;
import com.kato.pro.sensitive.service.ISensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 敏感词服务实现
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl implements ISensitiveWordService {

    @Resource
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.sensitive-update:sensitive-word-update}")
    private String sensitiveUpdateTopic;

    @Override
    public IPage<SensitiveWord> pageSensitiveWords(IPage<SensitiveWord> page, SensitiveWord sensitiveWord) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (sensitiveWord != null) {
            if (sensitiveWord.getWord() != null && !sensitiveWord.getWord().isEmpty()) {
                wrapper.like(SensitiveWord::getWord, sensitiveWord.getWord());
            }
            if (sensitiveWord.getLevel() != null && !sensitiveWord.getLevel().isEmpty()) {
                wrapper.eq(SensitiveWord::getLevel, sensitiveWord.getLevel());
            }
            if (sensitiveWord.getStatus() != null && !sensitiveWord.getStatus().isEmpty()) {
                wrapper.eq(SensitiveWord::getStatus, sensitiveWord.getStatus());
            }
        }
        wrapper.orderByDesc(SensitiveWord::getId);
        return sensitiveWordMapper.selectPage(page, wrapper);
    }

    @Override
    public SensitiveWord getById(Long id) {
        return sensitiveWordMapper.selectById(id);
    }

    @Override
    public boolean save(SensitiveWord sensitiveWord) {
        boolean result = sensitiveWordMapper.insert(sensitiveWord) > 0;
        if (result) {
            sendKafkaNotice();
        }
        return result;
    }

    @Override
    public boolean updateById(SensitiveWord sensitiveWord) {
        boolean result = sensitiveWordMapper.updateById(sensitiveWord) > 0;
        if (result) {
            sendKafkaNotice();
        }
        return result;
    }

    @Override
    public boolean removeById(Long id) {
        boolean result = sensitiveWordMapper.deleteById(id) > 0;
        if (result) {
            sendKafkaNotice();
        }
        return result;
    }

    @Override
    public void sendKafkaNotice() {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(sensitiveUpdateTopic, "update", "sensitive word updated");
            kafkaTemplate.send(record);
            log.info("发送敏感词更新Kafka通知成功, topic={}", sensitiveUpdateTopic);
        } catch (Exception e) {
            log.error("发送敏感词更新Kafka通知失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(List<SensitiveWord> sensitiveWords) {
        if (sensitiveWords == null || sensitiveWords.isEmpty()) {
            return false;
        }
        for (SensitiveWord word : sensitiveWords) {
            sensitiveWordMapper.insert(word);
        }
        sendKafkaNotice();
        log.info("批量保存敏感词成功，数量={}", sensitiveWords.size());
        return true;
    }

    @Override
    public List<SensitiveWord> listWords(SensitiveWord sensitiveWord) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (sensitiveWord != null) {
            if (sensitiveWord.getWord() != null && !sensitiveWord.getWord().isEmpty()) {
                wrapper.like(SensitiveWord::getWord, sensitiveWord.getWord());
            }
            if (sensitiveWord.getLevel() != null && !sensitiveWord.getLevel().isEmpty()) {
                wrapper.eq(SensitiveWord::getLevel, sensitiveWord.getLevel());
            }
            if (sensitiveWord.getStatus() != null && !sensitiveWord.getStatus().isEmpty()) {
                wrapper.eq(SensitiveWord::getStatus, sensitiveWord.getStatus());
            }
            if (sensitiveWord.getCategory() != null && !sensitiveWord.getCategory().isEmpty()) {
                wrapper.eq(SensitiveWord::getCategory, sensitiveWord.getCategory());
            }
        }
        wrapper.orderByDesc(SensitiveWord::getId);
        return sensitiveWordMapper.selectList(wrapper);
    }
}
