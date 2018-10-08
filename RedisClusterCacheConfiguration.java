import io.lettuce.core.ClientOptions.DisconnectedBehavior;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.embedded.RedisCluster;
import redis.embedded.RedisClusterBuilder;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Data
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(prefix = "data.rediscluster", name = {"nodes", "ttl", "redirects"})
@ConfigurationProperties(prefix = "data.rediscluster")
public class RedisClusterCacheConfiguration implements CacheBackendConfiguration {

	private List<String> nodes;
	private int redirects;
	private int ttl;
	private boolean embeded;

	private String password;

	/**
	 * Create an embedded redis cluster
	 */
	public class EmbeddedRedisCluster implements InitializingBean, DisposableBean {

		private RedisCluster redisCluster;
		private final static int SLAVES_PER_MASTER = 0;

		@Override
		public void afterPropertiesSet() {
			if (embeded) {
				RedisClusterBuilder builder = RedisCluster.builder();
				IntStream.range(0, nodes.size()).forEach(i -> {
					String[] nodeArray = nodes.get(i).split(":");
					builder.serverPorts(Arrays.asList(Integer.valueOf(nodeArray[1])))
							.replicationGroup(String.format("master%s", i), SLAVES_PER_MASTER);
				});

				redisCluster = builder.build();
				redisCluster.start();
			}
		}

		@Override
		public void destroy() {
			if (redisCluster != null) {
				redisCluster.stop();
			}
		}
	}

	@Bean
	public EmbeddedRedisCluster embeddedRedisCluster() {
		return new EmbeddedRedisCluster();
	}

	@Bean
	public LettuceClientConfiguration lettuceClientConfiguration() {
		SocketOptions socketOptions = SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build();
		ClusterTopologyRefreshOptions refreshOptions = ClusterTopologyRefreshOptions.builder()
				.enableAllAdaptiveRefreshTriggers().build();
		ClusterClientOptions clientOptions = ClusterClientOptions.builder()
				.socketOptions(socketOptions)
				.pingBeforeActivateConnection(true)
				.maxRedirects(redirects)
				.autoReconnect(true)
				.cancelCommandsOnReconnectFailure(true)
				.disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)
				.topologyRefreshOptions(refreshOptions)
				.build();
		return LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(15)).clientOptions(clientOptions)
				.build();
	}

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		if (embeded) {
			return new LettuceConnectionFactory(redisStandaloneConfiguration(), lettuceClientConfiguration());
		} else {
			return new LettuceConnectionFactory(redisClusterConfiguration(), lettuceClientConfiguration());
		}
	}

	@DependsOn({"embeddedRedisCluster"})
	public RedisStandaloneConfiguration redisStandaloneConfiguration() {
		String[] nodeArray = nodes.get(0).split(":");
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		redisStandaloneConfiguration.setHostName(nodeArray[0]);
		redisStandaloneConfiguration.setPort(Integer.valueOf(nodeArray[1]));
		redisStandaloneConfiguration.setPassword(getRedisPassword());
		return redisStandaloneConfiguration;
	}

	@Bean
	public RedisClusterConfiguration redisClusterConfiguration() {
		RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
		for (String node : nodes) {
			String[] nodeArray = node.split(":");
			RedisClusterNode redisNode = new RedisClusterNode(nodeArray[0], Integer.parseInt(nodeArray[1]));
			redisClusterConfiguration.addClusterNode(redisNode);
		}
		redisClusterConfiguration.setMaxRedirects(redirects);
		redisClusterConfiguration.setPassword(getRedisPassword());
		return redisClusterConfiguration;
	}

	@Bean
	public StringRedisSerializer stringRedisSerializer() {
		return new StringRedisSerializer();
	}

	@Bean
	public SnappyRedisSerializer genericSnappyRedisSerializer() {
		return new SnappyRedisSerializer<>(new KryoRedisSerializer<>());
	}

	@Bean
	public RedisCacheWriter redisCacheWriter() {
		return RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory());
	}

	@Bean
	public RedisCacheConfiguration defaultRedisCacheConfiguration() {

		return RedisCacheConfiguration.defaultCacheConfig()
				.disableKeyPrefix()
				.disableCachingNullValues()
				.serializeKeysWith(RedisSerializationContext
						.SerializationPair.fromSerializer(stringRedisSerializer()))
				.serializeValuesWith(RedisSerializationContext
						.SerializationPair.fromSerializer(genericSnappyRedisSerializer()))
				.entryTtl(Duration.ofSeconds(ttl));
	}

	private RedisPassword getRedisPassword() {
		return StringUtils.isNotEmpty(password) ? RedisPassword.of(password) : RedisPassword.none();
	}

}