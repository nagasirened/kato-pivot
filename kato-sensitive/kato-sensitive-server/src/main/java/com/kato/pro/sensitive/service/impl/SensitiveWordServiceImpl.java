package com.kato.pro.sensitive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.common.entity.LoginUser;
import com.kato.pro.common.resolver.LoginUserContextHolder;
import com.kato.pro.common.utils.AddrUtil;
import com.kato.pro.sensitive.entity.SensitiveWord;
import com.kato.pro.sensitive.mapper.SensitiveWordMapper;
import com.kato.pro.sensitive.service.ISensitiveOperationLogService;
import com.kato.pro.sensitive.service.ISensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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

    @Resource
    private ISensitiveOperationLogService operationLogService;

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
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SensitiveWord sensitiveWord) {
        // 默认操作人，便于审计追溯（外部调用方可显式覆写）
        if (sensitiveWord.getCreatorId() == null) {
            sensitiveWord.setCreatorId(currentOperatorId());
        }
        boolean result = sensitiveWordMapper.insert(sensitiveWord) > 0;
        if (result) {
            recordLog("CREATE", sensitiveWord.getId(), sensitiveWord.getWord(), null, sensitiveWord);
            sendKafkaNotice();
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SensitiveWord sensitiveWord) {
        Assert.notNull(sensitiveWord.getId(), "更新敏感词必须携带ID");
        SensitiveWord before = sensitiveWordMapper.selectById(sensitiveWord.getId());
        boolean result = sensitiveWordMapper.updateById(sensitiveWord) > 0;
        if (result) {
            // 入参 sensitiveWord 即为更新后的目标态，直接作为 after 落日志，避免重复查库
            recordLog("UPDATE", sensitiveWord.getId(), sensitiveWord.getWord(), before, sensitiveWord);
            sendKafkaNotice();
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Long id) {
        SensitiveWord before = sensitiveWordMapper.selectById(id);
        boolean result = sensitiveWordMapper.deleteById(id) > 0;
        if (result) {
            recordLog("DELETE", id, before != null ? before.getWord() : null, before, null);
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
        Integer operatorId = currentOperatorId();
        for (SensitiveWord word : sensitiveWords) {
            if (word.getCreatorId() == null) {
                word.setCreatorId(operatorId);
            }
            sensitiveWordMapper.insert(word);
            // 逐条记录CREATE日志，便于审计与按条回溯
            recordLog("CREATE", word.getId(), word.getWord(), null, word);
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

    /**
     * 统一写入操作日志（saveLog 内部已 @Async + try/catch，异常不会影响主业务流程）。
     * 注意：异步执行意味着主业务事务回滚时日志可能已落库，存在轻微不一致 ——
     * 若需强一致，可改为同步写日志或使用 TransactionalEventListener 阶段后置。
     */
    private void recordLog(String operationType, Long wordId, String wordContent, Object before, Object after) {
        operationLogService.saveLog(operationType, wordId, wordContent,
                currentOperatorId(), currentOperatorIp(), before, after);
    }

    /**
     * 从线程上下文取当前操作人ID。LoginUser.userId 是 String，但本系统 userId 为 Integer 自增，
     * 解析失败说明上下文污染，按异常处理并 log warn（不静默吞掉）。
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
     * 从当前 HTTP 请求里取客户端 IP（带反代头兼容）。
     */
    private String currentOperatorIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            return AddrUtil.getRemoteAddr(request);
        } catch (Exception e) {
            log.warn("获取客户端IP失败", e);
            return null;
        }
    }
}