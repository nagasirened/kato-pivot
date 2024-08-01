package com.kato.pro.uaa.handler.valid.saver;


import cn.hutool.core.text.CharSequenceUtil;
import com.kato.pro.uaa.entity.code.ValidateCode;
import org.springframework.web.context.request.ServletWebRequest;

public interface ICodeManager {

    String SAVE_KEY = "valid_code_key";
    String HEADER_VALID_CODE_KEY = "code_valid_prefix";

    default String wrapSaveKey(String randomPrefix) {
        return SAVE_KEY + ":" + randomPrefix;
    }
    /**
     * 保存验证码
     */
    void save(ServletWebRequest webRequest, ValidateCode validateCode);

    /**
     * 获取已经存储过的验证码
     */
    String getCode(String prefix, ServletWebRequest webRequest);

    /**
     * 校验验证码
     */
    default boolean checkCode(String code, ServletWebRequest webRequest) {
        String prefix = webRequest.getHeader(HEADER_VALID_CODE_KEY);
        if (CharSequenceUtil.isBlank(prefix)) {
            return false;
        }
        return code.equals(getCode(prefix, webRequest));
    }

}
