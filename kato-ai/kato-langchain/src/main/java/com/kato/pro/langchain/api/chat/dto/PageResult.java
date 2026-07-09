package com.kato.pro.langchain.api.chat.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应。MyBatis-Plus IPage → API PageResult 转换。
 * 不直接暴露 IPage 给前端（IPage 字段含敏感的 total/sql 等）。
 */
@Value
@Builder
public class PageResult<T> {
    long page;
    long size;
    long total;
    long totalPages;
    List<T> records;

    /** 通用转换：IPage<S> + S → T 映射函数 → PageResult<T> */
    public static <S, T> PageResult<T> of(IPage<S> p, Function<S, T> mapper) {
        return PageResult.<T>builder()
                .page(p.getCurrent())
                .size(p.getSize())
                .total(p.getTotal())
                .totalPages(p.getPages())
                .records(p.getRecords().stream().map(mapper).toList())
                .build();
    }
}
