package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kato.pro.langchain.domain.chat.ChatSession;
import com.kato.pro.langchain.domain.session.SessionStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * ChatSession Mapper。继承 TenantAwareBaseMapper → M1 的 TenantMybatisInterceptor
 * 自动注入 tenant_id 过滤条件。
 *
 * v1 暴露的查询：
 *   - selectUserActiveSessionPage: 当前用户下分页查询 ACTIVE 会话
 *   - selectUserSessionPageByStatus: 同上，可指定 status 过滤
 *   - casUpdateSummary: CAS 写回 summary（仅当 summary_version == expected 时更新）
 *
 * 复杂场景（如管理员看所有租户会话）v1 显式不走 mapper，而是用 @Interceptor 旁路
 * + 强制 SQL 带 tenant_id。
 */
@Mapper
public interface ChatSessionMapper extends TenantAwareBaseMapper<ChatSession> {

    /**
     * 分页查询当前用户的会话（仅 ACTIVE）。
     * 内部 Wrapper 会被 M1 拦截器追加 tenant_id 条件。
     */
    default IPage<ChatSession> selectUserActiveSessionPage(IPage<ChatSession> page, Long userId) {
        return selectPage(page,
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .eq("status", SessionStatus.ACTIVE.name())
                        .orderByDesc("updated_at"));
    }

    /**
     * 分页查询当前用户指定状态的会话。
     */
    default IPage<ChatSession> selectUserSessionPageByStatus(IPage<ChatSession> page, Long userId, String status) {
        return selectPage(page,
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .eq("status", status)
                        .orderByDesc("updated_at"));
    }

    /**
     * CAS 写回 summary：仅当 summary_version == expectedVersion 时更新，
     * 同时把 summary_version 自增 1。返回受影响行数（0=被并发挤掉）。
     */
    default int casUpdateSummary(Long sessionId, String newSummary, Integer expectedVersion) {
        return update(null, new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getSummaryVersion, expectedVersion)
                .set(ChatSession::getSummary, newSummary)
                .set(ChatSession::getSummaryVersion, expectedVersion + 1));
    }
}
