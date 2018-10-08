
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.xerial.snappy.Snappy;

import java.io.Serializable;

/**
 * Class used to compress/uncompress objects using snappy compression algorithm, and serialize/deserialize objects
 * to/from byte array.
 *
 * It supports Snappy for compression and either a custom {@link RedisSerializer} such as the {@link
 * KryoRedisSerializer}. If no custom serializer is provided then the @{link SerializationUtils} are used as default
 * serialiser.
 **/
public class SnappyRedisSerializer<T> implements RedisSerializer<T> {

	private RedisSerializer<T> innerSerializer;

	public SnappyRedisSerializer() {

	}

	public SnappyRedisSerializer(RedisSerializer<T> innerSerializer) {
		this.innerSerializer = innerSerializer;
	}

	/**
	 * Create a byte array by serialising and Compressing a java graph (object)
	 */
	@Override
	public byte[] serialize(T object) throws SerializationException {
		try {
			byte[] bytes = innerSerializer != null ? innerSerializer.serialize(object)
					: SerializationUtils.serialize((Serializable) object);
			return Snappy.compress(bytes);
		} catch (Exception e) {
			throw new SerializationException(e.getMessage(), e);
		}
	}

	/**
	 * Create a java graph (object) by uncompressing and deserializing a byte array
	 */
	@Override
	public T deserialize(byte[] bytes) throws SerializationException {
		try {
			byte[] bos = Snappy.uncompress(bytes);
			return (T) (innerSerializer != null ? innerSerializer.deserialize(bos) : SerializationUtils.deserialize(bos));
		} catch (Exception e) {
			throw new SerializationException(e.getMessage(), e);
		}
	}
}