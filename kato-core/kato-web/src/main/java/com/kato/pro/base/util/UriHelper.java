package com.kato.pro.base.util;

import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UriHelper {

    public static String buildGetUri(String uri, String... params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri);
        for (String param : params) {
            builder.queryParam(param);
        }
        return builder.build().toUriString();
    }

    public static String buildGetUri(String uri, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        return builder.build().toUriString();
    }

    public static String buildGetUriWithPathVariable(String uri, List<String> pathVariable, String... params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri);
        for (String param : params) {
            builder.queryParam(param);
        }
        builder.buildAndExpand(pathVariable);
        return builder.build().toUriString();
    }

    public static Map<String, String> parseUri(String url) {
        Map<String, String> params = new HashMap<>();
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(url).build();
        MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
        queryParams.forEach((key, value) -> params.put(key, value.get(0)));
        return params;
    }
}
