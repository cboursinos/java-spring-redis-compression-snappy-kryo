import data.cache.CacheSpecs;
import data.cache.RedisClusterCacheConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Map;

public class RedisClusterCacheManagerBuilder implements CacheManagerBuilder<RedisClusterCacheConfiguration> {

	@Override
	public CacheManager build(Map<String, CacheSpecs> specs,
			RedisClusterCacheConfiguration redisClusterCacheConfiguration) {
		RedisCacheManager manager = new RedisCacheManager(redisClusterCacheConfiguration.redisCacheWriter(),
				redisClusterCacheConfiguration.defaultRedisCacheConfiguration());
		manager.initializeCaches();
		return manager;
	}

}
