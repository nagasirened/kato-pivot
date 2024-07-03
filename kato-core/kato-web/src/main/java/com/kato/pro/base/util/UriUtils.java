package com.kato.pro.base.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UriUtils {

    public static Map<String, String> parseUrlParams(String url) {
        Map<String, String> queryPairs = new HashMap<>();
        try {
            URL urlObj = new URL(url);
            String query = urlObj.getQuery();

            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if ( idx < 0 ) continue;
                    queryPairs.put( URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            }
        } catch ( Exception e ) {
            log.warn("parse params from referPageUrl fail: url: {}， info: {}", url, e.getMessage() );
        }
        return queryPairs;
    }

}
