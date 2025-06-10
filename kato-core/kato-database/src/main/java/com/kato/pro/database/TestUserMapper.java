package com.kato.pro.database;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TestUserMapper {


    // 使用默认数据源
    @Select("SELECT id FROM users")
    List<Integer> findAll();

    // 使用第二个数据源
    @DataSource(DataSourceType.SECONDARY)
    @Select("SELECT * FROM external_users")
    List<Integer> findAllExternal();

}
