package sneer.commons;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings({"rawtypes", "unchecked"})
public class InteractiveSerializer {

	private ConcurrentMap<Class<?>, ObjectReplacer> localTypes = new ConcurrentHashMap<Class<?>, ObjectReplacer>();
	private ConcurrentMap<Class<?>, ObjectReplacer> remoteTypes = new ConcurrentHashMap<Class<?>, ObjectReplacer>();

	public <LocalType, RemoteType extends LocalType> void registerReplacer(Class<LocalType> local, Class<RemoteType> remote, ObjectReplacer<LocalType, RemoteType> replacer) {
		this.localTypes.put(local, replacer);
		this.remoteTypes.put(remote, replacer);
	}

	public byte[] serialize(Object obj) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out = new ObjectOutputStream(bytes) {
				{
					if (!localTypes.isEmpty()) {
						enableReplaceObject(true);
					}
				}

				@Override
				protected Object replaceObject(Object obj) throws IOException {
					ObjectReplacer replacer = resolveAndCacheInstanceOf(localTypes, obj.getClass());
					return replacer != null ? replacer.outgoing(obj) : super.replaceObject(obj); 
				}
			};
			out.writeObject(obj);
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return bytes.toByteArray();
	}
	
	public static <T> T resolveAndCacheInstanceOf(Map<Class<?>, T> map, Class<?> originalClass) {
		Class<?> candidate = originalClass;
		T ret = null;
		try {
			while (candidate != Object.class) {
				ret = map.get(candidate);
				if (ret != null) return ret;
				Class<?>[] ifaces = candidate.getInterfaces();
				for (Class<?> iface : ifaces) {
					ret = map.get(iface);
					if (ret != null) {
						candidate = iface;
						return ret;
					}
				}
				candidate = candidate.getSuperclass();
			}
		} finally {
			if (ret != null && candidate != originalClass) {
				map.put(candidate, ret);
			}
		}
		return ret;
	}

	public Object deserialize(byte[] bytes) {
		try {
			return new ObjectInputStream(new ByteArrayInputStream(bytes)) {
				{
					if (!remoteTypes.isEmpty()) {
						enableResolveObject(true);
					}
				}
				@Override
				protected Object resolveObject(Object obj) throws IOException {
					ObjectReplacer replacer = resolveAndCacheInstanceOf(remoteTypes, obj.getClass());
					return replacer != null ? replacer.incoming(obj) : super.resolveObject(obj); 
				};
			}.readObject();
		} catch (OptionalDataException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
