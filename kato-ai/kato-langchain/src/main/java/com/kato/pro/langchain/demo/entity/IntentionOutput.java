package com.kato.pro.langchain.demo.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class IntentionOutput {

    @Description("意图分析：1丢失信息登记;2失物登记;3失物查询;4其他")
    private Integer intention;

    @Description("大模型对用户的输出")
    private String output;

}
