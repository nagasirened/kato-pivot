package com.kato.pro.langchain.domain.tool;

/**
 * 工具类型。
 *
 *   - READ  : 读类；userId/tenantId 透传，直接调用 Tool.execute(args, ctx)
 *   - WRITE : 写类；进入审核队列，由 admin approve 后再真正执行
 */
public enum ToolType {
    READ, WRITE
}
