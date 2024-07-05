package com.kato.pro.base.util;

import com.kato.pro.base.entity.KatoHeader;
import com.kato.pro.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class RequestUtil {

    static List<Field> fields;
    public static final ThreadLocal<KatoHeader> holder = new InheritableThreadLocal<>();
    static {
        Field[] declaredFields = KatoHeader.class.getDeclaredFields();
        fields = Stream.of(declaredFields).peek(item -> item.setAccessible(true)).collect(Collectors.toList());
    }

    private RequestUtil() {}

    public static boolean hasHeader() {
        return Objects.nonNull(holder.get());
    }

    public static void setHeader(KatoHeader header) {
        if (Objects.isNull(header)) {
            reset();
        } else {
            holder.set(header);
        }
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
     * 释放
     */
    public static void reset() {
        holder.remove();
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
            for (Field declaredField : fields) {
                String fieldName = declaredField.getName();
                declaredField.set(katoHeader, request.getHeader(fieldName));
            }
        } catch (Exception e) {
            log.error("initHeader fail, request: {}", JsonUtils.toJSONString(request));
            return null;
        }
        return katoHeader;
    }

}