package com.kato.pro.langchain.infrastructure.persistence;

import com.kato.pro.langchain.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends TenantAwareBaseMapper<User> {
}
