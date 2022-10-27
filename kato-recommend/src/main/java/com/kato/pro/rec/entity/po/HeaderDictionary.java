package com.kato.pro.rec.entity.po;

import lombok.Data;

@Data
public class HeaderDictionary {

    /**
     * base
     */
    private String deviceId;
    private String ip;
    private String platform;
    private String platformVersion;

    /**
     * local
     */
    private String province;
    private String city;

    /**
     * user
     */
    private String ethnicity;
    private String language;

}
