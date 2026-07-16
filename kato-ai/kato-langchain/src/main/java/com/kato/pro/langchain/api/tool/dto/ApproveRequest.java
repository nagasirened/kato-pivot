package com.kato.pro.langchain.api.tool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
@Schema(description = "审批请求体")
public class ApproveRequest {

    @Size(max = 500, message = "comment 长度不能超过 500")
    @Schema(description = "审批意见（可空，记录到 audit）",
            example = "已确认合规",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            maxLength = 500)
    String comment;
}
