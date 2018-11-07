# java-spring-redis-compression-snappy-kryo #

Here is a sample of a Java Spring Boot Redis Cluster Data configuration. 

It is an implementation with Redis Cluster and Redis Cache Manager.

1. Snappy Compression 
2. Kryo Serialization 
3. Support ttl per cache key


### Gradle configuration ###

1. spring-data-redis
2. lettuce-core
3. snappy-java
4. kryo
5. commons-codec

### Application properties ###

~~~
data.rediscluster.nodes=redis1.foo.com:6379,redis2.foo.com:6379,redis3.foo.com:6379,redis4.foo.com:6379,redis5.foo.com:6379,redis6.foo.com:6379
data.rediscluster.ttl=2700
data.rediscluster.redirects=3
~~~

### How to use it ###

~~~
RedisCacheConfiguration.defaultCacheConfig()
.disableKeyPrefix()
.disableCachingNullValues()
.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringRedisSerializer()))
.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericSnappyRedisSerializer()))
.entryTtl(Duration.ofSeconds(ttl));
~~~
