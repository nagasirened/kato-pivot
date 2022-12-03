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

    /** 用户ID */
    private String userId;
    /** 设备号 */
    private String deviceId;
    /** maxNumber */
    private Integer topK;
    /** ab参数 */
    private Map<String, String> abMap = new HashMap<>();
    /** header解析的参数 */
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
