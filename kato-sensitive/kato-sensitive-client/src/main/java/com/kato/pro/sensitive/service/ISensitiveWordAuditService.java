package com.kato.pro.sensitive.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kato.pro.sensitive.entity.SensitiveWordAudit;

/**
 * 敏感词审核服务接口
 */
public interface ISensitiveWordAuditService {

    /**
     * 提交审核申请
     */
    boolean apply(SensitiveWordAudit audit);

    /**
     * 分页查询待审核列表
     */
    IPage<SensitiveWordAudit> pagePending(int pageNum, int pageSize);

    /**
     * 审批通过
     */
    boolean approve(Long auditId, Integer auditorId, String auditRemark);

    /**
     * 审批拒绝
     */
    boolean reject(Long auditId, Integer auditorId, String auditRemark);
}
