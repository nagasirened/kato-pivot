package com.kato.pro.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.kato.pro.redis.KatoRedisProperties;
import com.kato.pro.redis.RedisLockUtil;
import com.kato.pro.redis.RedisService;

import java.time.Duration;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(KatoRedisProperties.class)
@ConditionalOnProperty(value = KatoRedisProperties.PREFIX + ".enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfiguration {

	@SuppressWarnings("all")
	@Primary
	@Bean(name = "redisTemplate")
	@ConditionalOnClass(RedisOperations.class)
	public RedisTemplate redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate template = new RedisTemplate();
		template.setConnectionFactory(factory);
		Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
		ObjectMapper om = new ObjectMapper();
		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		om.activateDefaultTyping(om.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer.setObjectMapper(om);

		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		// key采用String的序列化方式
		template.setKeySerializer(stringRedisSerializer);
		// hash的key也采用String的序列化方式
		template.setHashKeySerializer(stringRedisSerializer);
		// value序列化方式采用jackson
		template.setValueSerializer(jackson2JsonRedisSerializer);
		// hash的value序列化方式采用jackson
		template.setHashValueSerializer(jackson2JsonRedisSerializer);
		template.afterPropertiesSet();
		return template;
	}

	@Bean
	@ConditionalOnBean(name = "redisLockUtil")
	public RedisLockUtil redisLockUtil(@Autowired RedisTemplate<String, String> redisTemplate) {
		return new RedisLockUtil(redisTemplate);
	}

	@Bean
	@ConditionalOnBean(name = "redisService")
	public RedisService redisService(RedisLockUtil redisLockUtil) {
		return new RedisService(redisLockUtil);
	}

	/**
	 * 当操作 redis 得接口一段时间没有调用后（比如30分钟），再次调用 redis 操作后，就会遇到连接超时得问题，导致接口异常。
	 * 项目中 redis 连接创建后，一段时间未传输数据后，客户端发送 psh 包，未收到服务端 ack 包，触发tcp得超时重传机制，在重传次数重试完后，最终客户端主动关闭了连接。
	 *
	 * 1.启用一个心跳定时任务，定时访问 redis，保持 redis 连接不被关闭，简而言之，就是写一个定时任务，定时调用 redis得 get 命令，进而保活 redis 连接
	 * 2.基于Springboot 提供得 LettuceClientConfigurationBuilderCustomizer 自定义客户端配置，博主这里主要针对第三种自定义客户端配置来讲解一种优雅得方式
	 */
	@Bean
	public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer() {
		return clientConfigurationBuilder -> {
			LettuceClientConfiguration clientConfiguration = clientConfigurationBuilder.build();
			ClientOptions clientOptions = clientConfiguration.getClientOptions().orElseGet(ClientOptions::create);
			ClientOptions build = clientOptions.mutate().build();
			SocketOptions.KeepAliveOptions.Builder builder = build.getSocketOptions().getKeepAlive().mutate();
			// 保活配置
			builder.enable(true);
			builder.idle(Duration.ofSeconds(30));
			SocketOptions.Builder socketOptionsBuilder = clientOptions.getSocketOptions().mutate();
			SocketOptions.KeepAliveOptions keepAliveOptions = builder.build();
			socketOptionsBuilder.keepAlive(keepAliveOptions);

			SocketOptions socketOptions = socketOptionsBuilder.build();
			ClientOptions clientOptionsPost = ClientOptions.builder().socketOptions(socketOptions).build();
			clientConfigurationBuilder.clientOptions(clientOptionsPost);
		};
	}

}