package data.cache.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayOutputStream;

@Slf4j
public class KryoSerializer<T> {

	private static final KryoFactory factory = new KryoFactory() {

		public Kryo create() {
			Kryo kryo = new Kryo();
			try {

				kryo.setRegistrationRequired(false);
				//NPE bug fix (Collection  deserialization)
				((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy())
						.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return kryo;
		}
	};


	private static final KryoPool pool = new KryoPool.Builder(factory).softReferences().build();

	/**
	 *
	 * @param obj
	 * @return
	 */
	public static byte[] serialize(final Object obj) {

		return pool.run(new KryoCallback<byte[]>() {

			@Override
			public byte[] execute(Kryo kryo) {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				Output output = new Output(stream);
				kryo.writeClassAndObject(output, obj);
				output.close();
				return stream.toByteArray();
			}

		});
	}

	/**
	 *
	 * @param objectData
	 * @param <V>
	 * @return
	 */
	public static <V> V deserialize(final byte[] objectData) {

		return (V) pool.run(new KryoCallback<V>() {

			@Override
			public V execute(Kryo kryo) {
				Input input = new Input(objectData);
				return (V) kryo.readClassAndObject(input);
			}

		});
	}

	/**
	 * Deep copy
	 *
	 * @param obj
	 * @param <V>
	 * @return
	 */
	public static <V> V deepCopy(final V obj) {

		return (V) pool.run(new KryoCallback<V>() {
			@Override
			public V execute(Kryo kryo) {
				return (V) kryo.copy(obj);
			}

		});
	}

}