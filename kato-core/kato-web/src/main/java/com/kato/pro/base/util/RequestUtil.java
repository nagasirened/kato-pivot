package com.kato.pro.base.util;

import com.kato.pre.base.util.JsonUtils;
import com.kato.pro.base.entity.KatoHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Objects;

@Slf4j
public final class RequestUtil {

    private RequestUtil() {}

    public static final InheritableThreadLocal<KatoHeader> holder = new InheritableThreadLocal<>();

    public static void setHeader(KatoHeader header) {
        if (Objects.isNull(header)) {
            reset();
        } else {
            holder.set(header);
        }
    }

    public static void reset() {
        holder.remove();
    }

    public static KatoHeader getHeader() {
        KatoHeader header = holder.get();
        if (Objects.isNull(header)) {
            header = initHeader();
            setHeader(header);
        }
        return header;
    }

    /**
     * 封装header对象
     */
    private static KatoHeader initHeader() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Assert.notNull(servletRequestAttributes, "servletRequestAttributes is null");
        HttpServletRequest request = servletRequestAttributes.getRequest();
        KatoHeader katoHeader = new KatoHeader();
        try {
            for (Field declaredField : katoHeader.getClass().getDeclaredFields()) {
                String fieldName = declaredField.getName();
                declaredField.setAccessible(true);
                declaredField.set(katoHeader, request.getHeader(fieldName));
            }
        } catch (Exception e) {
            log.error("initHeader fail, request: {}", JsonUtils.toJSONString(request));
            return null;
        }
        return katoHeader;
    }

}