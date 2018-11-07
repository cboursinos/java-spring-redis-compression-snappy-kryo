import data.cache.CacheSpecs;
import data.cache.RedisClusterCacheConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Map;

public class RedisClusterCacheManagerBuilder implements CacheManagerBuilder<RedisClusterCacheConfiguration> {

	@Override
	public CacheManager build(Map<String, CacheSpecs> specs,
			RedisClusterCacheConfiguration redisClusterCacheConfiguration) {

		RedisCacheManager manager =  RedisCacheManager.builder(redisClusterCacheConfiguration.redisCacheWriter())
				.cacheDefaults(redisClusterCacheConfiguration.defaultRedisCacheConfiguration())
				.withInitialCacheConfigurations(geCacheConfigurations(specs, redisClusterCacheConfiguration))
				.build();
		manager.initializeCaches();
		return manager;
	}

	private Map<String, RedisCacheConfiguration> geCacheConfigurations(Map<String, CacheSpecs> specs,
			RedisClusterCacheConfiguration redisClusterCacheConfiguration) {
		Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
		RedisCacheConfiguration defaultCacheConfiguration = redisClusterCacheConfiguration.defaultRedisCacheConfiguration();
		specs.entrySet().forEach(entry -> {
			cacheConfigurations
					.put(entry.getKey(), defaultCacheConfiguration.entryTtl(Duration.ofSeconds(entry.getValue().getTtl())));
		});
		return cacheConfigurations;
	}

}
