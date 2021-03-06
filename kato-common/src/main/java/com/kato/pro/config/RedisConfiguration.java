package com.kato.pro.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.redis.KatoRedisProperties;
import com.kato.pro.redis.RedisLockUtil;
import com.kato.pro.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableConfigurationProperties(KatoRedisProperties.class)
@ConditionalOnProperty(value = KatoRedisProperties.PREFIX + ".enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfiguration {

	@SuppressWarnings("all")
	@Primary
	@Bean(name = "redisTemplate")
	@ConditionalOnClass(RedisOperations.class)
	public org.springframework.data.redis.core.RedisTemplate redisTemplate(RedisConnectionFactory factory) {
		org.springframework.data.redis.core.RedisTemplate template = new org.springframework.data.redis.core.RedisTemplate();
		template.setConnectionFactory(factory);

		Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
		ObjectMapper om = new ObjectMapper();
		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		om.activateDefaultTyping(om.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer.setObjectMapper(om);

		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		// key??????String??????????????????
		template.setKeySerializer(stringRedisSerializer);
		// hash???key?????????String??????????????????
		template.setHashKeySerializer(stringRedisSerializer);
		// value?????????????????????jackson
		template.setValueSerializer(jackson2JsonRedisSerializer);
		// hash???value?????????????????????jackson
		template.setHashValueSerializer(jackson2JsonRedisSerializer);
		template.afterPropertiesSet();
		return template;
	}

	@Bean
	@ConditionalOnBean(name = "redisLockUtil")
	public RedisLockUtil redisLockUtil(@Autowired RedisTemplate redisTemplate) {
		return new RedisLockUtil(redisTemplate);
	}

	@Bean
	@ConditionalOnBean(name = "redisService")
	public RedisService redisService(RedisLockUtil redisLockUtil) {
		return new RedisService(redisLockUtil);
	}



}