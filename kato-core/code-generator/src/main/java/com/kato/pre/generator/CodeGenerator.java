package com.kato.pre.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.sql.Types;

public class CodeGenerator {

    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/user-center", "root", "root")
                .globalConfig(builder ->
                    builder.author("guangfu.zeng")
                            .enableSwagger()
                            .outputDir("D:/generator-codes/")
                )
                .dataSourceConfig(builder ->
                    builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                        int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                        if (typeCode == Types.SMALLINT) {
                            return DbColumnType.INTEGER;
                        }
                        return typeRegistry.getColumnType(metaInfo);
                    })
                )
                .packageConfig(builder ->  // parent.moduleName.XXX
                    builder.parent("com.kato.pro")
                            .moduleName("recommend")
                            .service("service")
                            .serviceImpl("service.impl")
                            .controller("controller")
                            .mapper("dao")
                            .entity("entity")
                )
                .strategyConfig(builder ->
                    builder.addInclude("sys_menu")
                            .addTablePrefix("sys_")
                )
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }

}
