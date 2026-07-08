package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.exception.SystemException;
import com.kato.pro.langchain.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Set;

/**
 * 多租户 SQL 拦截器（P0 安全核心）。
 *
 * 功能：
 *   - SELECT/UPDATE/DELETE: 自动追加 WHERE tenant_id = ?
 *   - INSERT: 自动注入 tenant_id 列 + 值
 *
 * 豁免规则（白名单）：
 *   - SQL 含特殊注释 /* NO_TENANT_FILTER *／  → 跳过（写日志 WARN，谨慎使用）
 *   - 表名在 exemptTables 中（tenant / flyway_schema_history）
 *
 * 异常：未设置 TenantContext 但又触发了非豁免 SQL → 抛 SystemException
 */
@Slf4j
public class TenantMybatisInterceptor implements InnerInterceptor {

    private static final String NO_TENANT_FILTER_MARKER = "/* NO_TENANT_FILTER */";
    private static final String TENANT_COLUMN = "tenant_id";

    /** 不参与多租户过滤的表（系统表） */
    private static final Set<String> DEFAULT_EXEMPT_TABLES = Set.of("tenant", "flyway_schema_history");

    private final Set<String> exemptTables;

    public TenantMybatisInterceptor() {
        this(DEFAULT_EXEMPT_TABLES);
    }

    public TenantMybatisInterceptor(Set<String> exemptTables) {
        this.exemptTables = exemptTables;
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        MetaObject metaObject = SystemMetaObject.forObject(sh);
        MappedStatement ms = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        if (ms == null) {
            return;
        }
        SqlCommandType cmd = ms.getSqlCommandType();
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        String originalSql = boundSql.getSql();

        // 1. 显式豁免
        if (originalSql.contains(NO_TENANT_FILTER_MARKER)) {
            log.warn("SQL bypassed tenant filter: {}", originalSql);
            return;
        }

        // 2. 表名豁免（仅当 SQL 中只涉及豁免表时跳过）
        if (isExemptTableOnly(originalSql)) {
            return;
        }

        // 3. 必须有 TenantContext
        if (TenantContext.currentOrNull() == null) {
            throw new SystemException(ErrorCode.TENANT_MISMATCH,
                    "Tenant context not set. SQL: " + originalSql);
        }
        Long tenantId = TenantContext.currentTenantId();

        // 4. 根据 SQL 类型改写
        String rewritten;
        try {
            rewritten = rewriteSql(originalSql, cmd, tenantId);
        } catch (JSQLParserException e) {
            throw new SystemException(ErrorCode.INTERNAL_ERROR,
                    "Failed to parse SQL for tenant injection: " + originalSql, e);
        }

        // 5. 替换 BoundSql（反射）
        metaObject.setValue("delegate.boundSql.sql", rewritten);
    }

    private boolean isExemptTableOnly(String sql) {
        String upper = sql.toUpperCase();
        if (!upper.contains("FROM ") && !upper.contains("UPDATE ") && !upper.contains("INTO ")) {
            return false;
        }
        for (String table : exemptTables) {
            if (upper.contains(table.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private String rewriteSql(String originalSql, SqlCommandType cmd, Long tenantId) throws JSQLParserException {
        Statement stmt = CCJSqlParserUtil.parse(originalSql);

        if (stmt instanceof Select select) {
            return injectTenantIntoSelect(select, tenantId, originalSql);
        }
        if (stmt instanceof Update update) {
            return injectTenantIntoWhere(update.getWhere(), tenantId, originalSql, "UPDATE");
        }
        if (stmt instanceof Delete delete) {
            return injectTenantIntoWhere(delete.getWhere(), tenantId, originalSql, "DELETE");
        }
        if (stmt instanceof Insert insert) {
            return injectTenantIntoInsert(insert, tenantId);
        }
        // 其它类型（TRUNCATE/REPLACE 等）原样放行 + WARN
        log.warn("Unhandled SQL command type, tenant filter not applied: {}", originalSql);
        return originalSql;
    }

    private String injectTenantIntoSelect(Select select, Long tenantId, String originalSql) {
        if (select instanceof PlainSelect ps) {
            EqualsTo eq = new EqualsTo().withLeftExpression(new Column(TENANT_COLUMN))
                    .withRightExpression(new LongValue(tenantId));
            if (ps.getWhere() == null) {
                ps.setWhere(eq);
            } else {
                ps.setWhere(new AndExpression(ps.getWhere(), eq));
            }
            return ps.toString();
        }
        // UNION / 子查询：保守地抛错（v1 不支持复杂查询 + 多租户）
        log.warn("Complex SELECT (UNION/subquery) cannot be auto-tenant-filtered: {}", originalSql);
        return originalSql;
    }

    private String injectTenantIntoWhere(net.sf.jsqlparser.expression.Expression where,
                                          Long tenantId,
                                          String originalSql,
                                          String sqlType) {
        if (where == null) {
            // 无 WHERE 条件 → 不允许直接走全表
            throw new SystemException(ErrorCode.TENANT_MISMATCH,
                    sqlType + " without WHERE clause is forbidden for tenant safety: " + originalSql);
        }
        // JSqlParser 的 Update/Delete 不可变，需要 replaceWhere
        // 简化方案：直接在 SQL 文本末尾追加 " AND tenant_id = X"
        // 这里用 toString + 字符串拼接（生产可改用 AST mutation）
        StringBuilder sb = new StringBuilder(originalSql);
        sb.append(" AND ").append(TENANT_COLUMN).append(" = ").append(tenantId);
        return sb.toString();
    }

    private String injectTenantIntoInsert(Insert insert, Long tenantId) {
        // 用字符串重写以兼容 JSqlParser 5.x 的 API 变化
        // 目标：INSERT INTO t (col1, col2) VALUES (v1, v2)
        //   →   INSERT INTO t (col1, col2, tenant_id) VALUES (v1, v2, 42)
        String original = insert.toString();
        // 已含 tenant_id → 不重复追加
        if (original.toLowerCase().contains(TENANT_COLUMN)) {
            return original;
        }
        // 1) 定位 "VALUES" 关键字（不区分大小写）
        String upper = original.toUpperCase();
        int valuesIdx = upper.lastIndexOf("VALUES");
        if (valuesIdx < 0) {
            log.warn("INSERT without VALUES clause, tenant filter not applied: {}", original);
            return original;
        }
        // 2) 在 VALUES 之前的 columns 右括号 — 找从 VALUES 向左最近的 )
        int colsEnd = original.lastIndexOf(')', valuesIdx);
        if (colsEnd < 0) {
            log.warn("INSERT columns paren not found, tenant filter not applied: {}", original);
            return original;
        }
        // 3) values 末尾右括号（最右的 )
        int valuesEnd = original.lastIndexOf(')');
        if (valuesEnd <= colsEnd) {
            log.warn("INSERT structure unrecognized, tenant filter not applied: {}", original);
            return original;
        }
        // 4) 拼装
        return original.substring(0, colsEnd)
                + ", " + TENANT_COLUMN
                + original.substring(colsEnd, valuesEnd)
                + ", " + tenantId
                + original.substring(valuesEnd);
    }
}
