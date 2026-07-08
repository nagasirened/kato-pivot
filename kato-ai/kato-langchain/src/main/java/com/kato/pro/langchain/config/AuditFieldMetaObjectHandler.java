package com.kato.pro.langchain.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.kato.pro.langchain.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充审计字段：
 *   - INSERT: createTime=now, updateTime=now, deleted=0
 *   - UPDATE: updateTime=now
 *
 * 注意：tenant_id 由 TenantMybatisInterceptor 注入到 SQL（fill=INSERT + 拦截器），不通过 MetaObjectHandler。
 */
@Slf4j
@Component
public class AuditFieldMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        log.debug("Audit insert fill: tenant={}", TenantContext.currentOrNull());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
