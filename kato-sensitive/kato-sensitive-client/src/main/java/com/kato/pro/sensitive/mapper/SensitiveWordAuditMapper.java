package com.kato.pro.sensitive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kato.pro.sensitive.entity.SensitiveWordAudit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词审核Mapper
 */
@Mapper
public interface SensitiveWordAuditMapper extends BaseMapper<SensitiveWordAudit> {
}
