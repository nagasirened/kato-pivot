package com.kato.pro.sensitive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.sensitive.entity.SensitiveWord;
import com.kato.pro.sensitive.entity.SensitiveWordAudit;
import com.kato.pro.sensitive.entity.constant.AuditStatus;
import com.kato.pro.sensitive.mapper.SensitiveWordAuditMapper;
import com.kato.pro.sensitive.mapper.SensitiveWordMapper;
import com.kato.pro.sensitive.service.ISensitiveWordAuditService;
import com.kato.pro.sensitive.service.SensitiveWordLoadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 敏感词审核服务实现
 */
@Slf4j
@Service
public class SensitiveWordAuditServiceImpl implements ISensitiveWordAuditService {

    @Resource
    private SensitiveWordAuditMapper auditMapper;

    @Resource
    private SensitiveWordMapper wordMapper;

    @Resource
    private SensitiveWordLoadService wordLoadService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean apply(SensitiveWordAudit audit) {
        if (audit == null || audit.getWord() == null || audit.getWord().isEmpty()) {
            return false;
        }

        // 设置审核状态为待审核
        audit.setAuditStatus(AuditStatus.PENDING.getCode());
        audit.setCreateTime(LocalDateTime.now());
        audit.setUpdateTime(LocalDateTime.now());

        int result = auditMapper.insert(audit);
        log.info("提交敏感词审核申请: word={}, result={}", audit.getWord(), result > 0);
        return result > 0;
    }

    @Override
    public IPage<SensitiveWordAudit> pagePending(int pageNum, int pageSize) {
        LambdaQueryWrapper<SensitiveWordAudit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensitiveWordAudit::getAuditStatus, AuditStatus.PENDING.getCode())
                .orderByAsc(SensitiveWordAudit::getCreateTime);

        Page<SensitiveWordAudit> page = new Page<>(pageNum, pageSize);
        return auditMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long auditId, Integer auditorId, String auditRemark) {
        SensitiveWordAudit audit = auditMapper.selectById(auditId);
        if (audit == null) {
            log.warn("审核记录不存在: auditId={}", auditId);
            return false;
        }

        if (!AuditStatus.PENDING.getCode().equals(audit.getAuditStatus())) {
            log.warn("审核记录状态不是待审核: auditId={}, status={}", auditId, audit.getAuditStatus());
            return false;
        }

        // 更新审核状态
        audit.setAuditStatus(AuditStatus.APPROVED.getCode());
        audit.setAuditorId(auditorId);
        audit.setAuditTime(LocalDateTime.now());
        audit.setAuditRemark(auditRemark);
        audit.setUpdateTime(LocalDateTime.now());
        auditMapper.updateById(audit);

        // 将敏感词添加到正式表
        SensitiveWord word = new SensitiveWord();
        word.setWord(audit.getWord());
        word.setLevel(audit.getLevel());
        word.setCategory(audit.getCategory());
        word.setStatus(audit.getStatus());
        word.setRemark(audit.getRemark());
        wordMapper.insert(word);

        // 重新加载敏感词到缓存/Trie树
        wordLoadService.hotReloadSensitiveWords();

        log.info("敏感词审核通过: auditId={}, word={}", auditId, audit.getWord());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long auditId, Integer auditorId, String auditRemark) {
        SensitiveWordAudit audit = auditMapper.selectById(auditId);
        if (audit == null) {
            log.warn("审核记录不存在: auditId={}", auditId);
            return false;
        }

        if (!AuditStatus.PENDING.getCode().equals(audit.getAuditStatus())) {
            log.warn("审核记录状态不是待审核: auditId={}, status={}", auditId, audit.getAuditStatus());
            return false;
        }

        // 更新审核状态
        audit.setAuditStatus(AuditStatus.REJECTED.getCode());
        audit.setAuditorId(auditorId);
        audit.setAuditTime(LocalDateTime.now());
        audit.setAuditRemark(auditRemark);
        audit.setUpdateTime(LocalDateTime.now());
        auditMapper.updateById(audit);

        log.info("敏感词审核拒绝: auditId={}, word={}", auditId, audit.getWord());
        return true;
    }
}
