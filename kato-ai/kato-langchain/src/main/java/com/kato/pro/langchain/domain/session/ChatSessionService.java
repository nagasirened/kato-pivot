package com.kato.pro.langchain.domain.session;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.domain.chat.ChatSession;
import com.kato.pro.langchain.infrastructure.persistence.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话服务。
 *
 * 设计原则：
 *   - 所有方法内部读 TenantContext 获取 tenantId/userId，调用方无需传
 *   - 写操作加 @Transactional（v1 简单单表事务；复杂场景 v2 加）
 *   - Mapper 走 TenantAwareBaseMapper → M1 拦截器自动加 tenant_id
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionMapper sessionMapper;

    public static final int MAX_TITLE_LENGTH = 255;

    /**
     * 创建新会话。
     */
    @Transactional
    public ChatSession createSession(String title) {
        String safeTitle = validateTitle(title);
        ChatSession s = new ChatSession();
        s.setUserId(TenantContext.currentUserId());
        s.setChannel("DEFAULT");
        s.setTitle(safeTitle);
        s.setStatus(SessionStatus.ACTIVE);
        s.setSummaryVersion(0);
        sessionMapper.insert(s);
        log.info("Session created: id={}, user={}, tenant={}",
                s.getId(), s.getUserId(), TenantContext.currentTenantId());
        return s;
    }

    /**
     * 查单个会话（自动 tenant 隔离）。
     */
    public ChatSession getSession(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "session id 不能为空");
        }
        ChatSession s = sessionMapper.selectById(id);
        if (s == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "会话不存在: id=" + id);
        }
        // 多一道防御：即便 mapper 拦截器失效，这里也校验
        if (!s.getUserId().equals(TenantContext.currentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return s;
    }

    /**
     * 分页查询当前用户的活跃会话。
     */
    public IPage<ChatSession> listUserActiveSessions(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        IPage<ChatSession> p = new Page<>(page, size);
        return sessionMapper.selectUserActiveSessionPage(p, TenantContext.currentUserId());
    }

    /**
     * 归档会话（ACTIVE → ARCHIVED）。
     */
    @Transactional
    public boolean archiveSession(Long id) {
        ChatSession s = getSession(id);
        if (!canArchive(s)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "会话已归档，无法重复归档: id=" + id);
        }
        s.setStatus(SessionStatus.ARCHIVED);
        int rows = sessionMapper.updateById(s);
        log.info("Session archived: id={}, user={}", id, TenantContext.currentUserId());
        return rows > 0;
    }

    /**
     * 内部用：M8 ChatEngine 每次写入消息后调用，更新 updated_at。
     */
    @Transactional
    public void touchSession(Long id) {
        ChatSession s = getSession(id);
        sessionMapper.updateById(s); // MyBatis-Plus MetaObjectHandler 自动填充 updateTime
    }


    /**
     * CAS 写回 session summary（summary_version 自增）。
     *
     * 调用方：HistorySummarizer。版本不匹配（被并发挤掉）时静默返回 false。
     */
    @Transactional
    public boolean updateSummary(Long sessionId, String newSummary, Integer expectedVersion) {
        if (sessionId == null || expectedVersion == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "sessionId/expectedVersion 不能为空");
        }
        String safe = newSummary == null ? "" : newSummary;
        if (safe.length() > 32_000) {
            // 防御：单条 summary 不超过 32K chars；超出截断
            safe = safe.substring(0, 32_000);
        }
        int rows = sessionMapper.casUpdateSummary(sessionId, safe, expectedVersion);
        log.debug("CAS updateSummary session={} expectedVersion={} rows={}", sessionId, expectedVersion, rows);
        return rows > 0;
    }

    // ===== 纯方法（无 DB） — 可单测 =====

    /**
     * 校验并清洗 title：null → ""，超过 MAX_TITLE_LENGTH 截断。
     */
    public String validateTitle(String title) {
        if (title == null) return "";
        String trimmed = title.trim();
        if (trimmed.isEmpty()) return "";
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            trimmed = trimmed.substring(0, MAX_TITLE_LENGTH);
        }
        return trimmed;
    }

    /**
     * 状态机：只能 ACTIVE → ARCHIVED。
     */
    public boolean canArchive(ChatSession s) {
        return s != null && s.getStatus() == SessionStatus.ACTIVE;
    }
}
