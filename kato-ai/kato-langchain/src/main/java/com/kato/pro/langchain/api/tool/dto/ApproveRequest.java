package com.kato.pro.langchain.api.tool.dto;

import lombok.Value;

@Value
public class ApproveRequest {
    /** 审核意见（可选，记录到 audit） */
    String comment;
}
