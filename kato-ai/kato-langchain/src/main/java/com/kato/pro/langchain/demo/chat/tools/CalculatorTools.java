package com.kato.pro.langchain.demo.chat.tools;


import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.exception.ToolExecutionException;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTools {

    /**
     * Tool注解中，如果name没有，默认就是用方法名称
     * value就是对该工具的解释、描述
     *
     * returnBehavior： TO_LLM（默认）	工具返回值发送回 LLM 继续处理，LLM 会基于结果生成最终回答
     *                  IMMEDIATE	    工具返回值直接返回给调用方，不经过 LLM 进一步处理
     *                  使用 IMMEDIATE 时，AI Service 方法的返回类型必须是 Result<T>
     *
     *  metadata   一个有效的 JSON 字符串，用于存储 LLM 提供商特定的工具元数据
     *             此元数据默认不会发送给 LLM 提供商 API，需要在创建 ChatModel 时显式指定哪些 metadata key 需要发送
     *
     *
     * 新版本的tool不需要一定返回ToolExecutionResult对象，而是直接返回结果就可以，框架就自动包装
     *
     */
    @Tool(value = "执行两个数字的加法运算")
    public Double sum(Double numA, Double numB) {
        System.out.println("普通加法");
        // return ToolExecutionResult.builder().result(numA + numB).isError(false).build();
        return numA + numB;
    }

    @Tool(value = "执行两个数字的除法运算", returnBehavior = ReturnBehavior.IMMEDIATE, metadata = "{\"priority\": \"high\", \"cacheable\": true}")
    public Double division(@P("除数") Double numA, @P("被除数") Double numB) {
        if (numB == 0) {
            throw new ToolExecutionException("除数不能为零");
        }
        System.out.println("普通除法");
        return numA / numB;
    }


    /**
     *
     ✨如果有多个@P 的字段，可以结构化地传递下面的参数✨
     ✨POJO 中的所有字段默认都是必需的，LLM 必须为每个字段提供值。✨
     ✨且支持嵌套✨

     @Data
     public class OrderRequest {
        private String productId;           // 必需
        private Integer quantity;           // 必需

        @JsonProperty(required = false)
        private String remark;              // 可选

        @JsonProperty(required = false)
        private Boolean giftWrapping;       // 可选
     }
     */

}