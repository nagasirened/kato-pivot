package com.kato.pro.risk.client.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控检查响应。
 *
 * Phase 1 骨架。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RiskResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 请求唯一 ID */
    private String requestId;

    /** 决策动作：PASS / REVIEW / BLOCK */
    private String action;

    /** 风险分（0-100，整数） */
    private Integer score;

    /** 命中的规则编码列表 */
    private List<String> reasonCodes;

    /** 扩展数据 */
    private Map<String, String> extData;

    /** 友好描述 */
    private String message;

    public static RiskResponse pass(String requestId) {
        RiskResponse r = new RiskResponse();
        r.setRequestId(requestId);
        r.setAction("PASS");
        r.setScore(0);
        r.setReasonCodes(new ArrayList<>());
        r.setExtData(new HashMap<>());
        return r;
    }

    public static RiskResponse failOpen(String requestId, String reasonCode) {
        RiskResponse r = new RiskResponse();
        r.setRequestId(requestId);
        r.setAction("PASS");
        r.setScore(0);
        r.setReasonCodes(new ArrayList<>());
        r.getReasonCodes().add(reasonCode);
        r.setExtData(new HashMap<>());
        return r;
    }

    public boolean isPass()    { return "PASS".equals(action); }
    public boolean isReview()  { return "REVIEW".equals(action); }
    public boolean isBlock()   { return "BLOCK".equals(action); }
}