package com.uci.utils;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.uci.utils.cdn.samagra.MinioClientProp;

import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.api.LoginRequest;

@Configuration
@EnableAutoConfiguration
public class UtilAppConfiguration {

	@Value("${campaign.url}")
	public String CAMPAIGN_URL;

	@Value("${campaign.admin.token}")
	public String CAMPAIGN_ADMIN_TOKEN;

	@Value("${caffeine.cache.max.size}")
	public Integer cacheMaxSize;
	
	@Value("${caffeine.cache.exprie.duration.seconds}")
	public Integer cacheExpireDuration;
	
	@Value("${spring.redis.database}")
	private String redisDb;
	
	@Value("${spring.redis.host}")
	private String redisHost;
	
	@Value("${spring.redis.port}")
	private String redisPort;
    
    @Value("${cdn.minio.login.id}")
	private String minioLoginId;
	
	@Value("${cdn.minio.password}")
	private String minioPassword;
	
	@Value("${cdn.minio.application.id}")
	private String minioAppId;
	
	@Value("${cdn.minio.bucket.id}")
	private String minioBucketId;
	
	@Value("${cdn.minio.url}")
	private String minioUrl;
	
	@Value("${cdn.minio.fa.key}")
	private String minioFAKey;
	
	@Value("${cdn.minio.fa.url}")
	private String minioFAUrl;
	
	public Caffeine<Object, Object> caffeineCacheBuilder() {
		return Caffeine.newBuilder()
				.maximumSize(cacheMaxSize)
				.expireAfterWrite(Duration.ofSeconds(cacheExpireDuration))
				.recordStats();
	}

	@Bean
	public Cache<Object, Object> cache() {
		return caffeineCacheBuilder().build();
	}

	@Bean
	public WebClient getWebClient() {
		return WebClient.builder().baseUrl(CAMPAIGN_URL).defaultHeader("admin-token", CAMPAIGN_ADMIN_TOKEN).build();
	}
	
	@SuppressWarnings("ALL")
	@Bean
	JedisConnectionFactory jedisConnectionFactory() {
		JedisConnectionFactory jedisConFactory
	      = new JedisConnectionFactory();
	    jedisConFactory.setHostName(redisHost);
	    Integer port = Integer.parseInt(redisPort);
	    jedisConFactory.setPort(port);
	    Integer dbIndex = Integer.parseInt(redisDb);
	    jedisConFactory.setDatabase(dbIndex);
	    return jedisConFactory;
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate() {
	    RedisTemplate<String, Object> template = new RedisTemplate<>();
	    template.setConnectionFactory(jedisConnectionFactory());
	    template.setKeySerializer(new StringRedisSerializer());
	    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
	    return template;
	}
	
	@Bean
	public MinioClientProp getMinioClientProp() {
		UUID applicationId = UUID.fromString(minioAppId);
		System.out.println("Minio FA key & url :"+minioFAKey+", "+minioFAUrl);
		return MinioClientProp.builder()
				.loginRequest(new LoginRequest(applicationId, minioLoginId, minioPassword))
				.cdnBaseUrl(minioUrl)
				.bucketId(minioBucketId)
				.fusionAuth(new FusionAuthClient(minioFAKey, minioFAUrl))
				.build();
	}
}
