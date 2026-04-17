package com.kato.pro.sensitive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kato.pro.sensitive.entity.SensitiveWordHitLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词命中日志Mapper
 */
@Mapper
public interface SensitiveWordHitLogMapper extends BaseMapper<SensitiveWordHitLog> {
}
