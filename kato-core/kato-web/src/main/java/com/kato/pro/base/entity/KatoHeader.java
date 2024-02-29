package com.kato.pro.base.entity;

import lombok.Data;

@Data
public class KatoHeader {

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
