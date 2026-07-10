package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.langchain.domain.sync.SyncRunRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SyncRunRecordMapper extends TenantAwareBaseMapper<SyncRunRecord> {

    /** 按 adapter 取最近 N 条 */
    default List<SyncRunRecord> findRecentByAdapter(String adapterName, int limit) {
        QueryWrapper<SyncRunRecord> q = new QueryWrapper<SyncRunRecord>()
                .eq("adapter_name", adapterName)
                .orderByDesc("start_time")
                .last("LIMIT " + Math.min(Math.max(limit, 1), 100));
        return selectList(q);
    }

    /** 分页查询全部 */
    default IPage<SyncRunRecord> page(int page, int size, String adapterName) {
        QueryWrapper<SyncRunRecord> q = new QueryWrapper<>();
        if (adapterName != null && !adapterName.isBlank()) q.eq("adapter_name", adapterName);
        q.orderByDesc("start_time");
        return selectPage(new Page<>(page, size), q);
    }
}
