package com.kato.pro.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(KatoRedisProperties.PREFIX)
public class KatoRedisProperties {
	/**
	 * 前缀
	 */
	public static final String PREFIX = "kato.lettuce.redis";
	/**
	 * 是否开启Lettuce
	 */
	private Boolean enable = true;
}
