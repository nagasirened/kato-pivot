package com.kato.pro.rec.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class RecommendRequest {

    /** 用户ID */
    private String userId;
    /** 设备号 */
    @NotEmpty(message = "device is empty")
    private String deviceId;
    /** maxNumber */
    private Integer topK = 10;
    /** ab参数 */
    private Map<String, String> abMap = new HashMap<>();
    /** 曝光的内容 */
    @JsonIgnore
    private Set<Integer> trash = new HashSet<>();
}
