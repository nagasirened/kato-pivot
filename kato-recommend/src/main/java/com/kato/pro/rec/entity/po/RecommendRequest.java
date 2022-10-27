package com.kato.pro.rec.entity.po;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.kato.pro.base.entity.CommonCode;
import com.kato.pro.base.exception.KatoServiceException;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class RecommendRequest {

    private String userId;
    private String deviceId;
    private Integer topK;
    private Map<String, String> abMap = new HashMap<>();
    private HeaderDictionary headerDictionary;

    /**
     * 参数解析后处理
     */
    public void check() {
        if (StrUtil.isBlank(deviceId) || ObjectUtil.isNull(headerDictionary)) {
            throw new KatoServiceException(CommonCode.PARAM_ERROR);
        }
        topK = ObjectUtil.isNull(topK) ? 10 : topK;
    }
}
