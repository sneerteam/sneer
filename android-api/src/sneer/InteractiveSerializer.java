package sneer;

import java.io.*;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class InteractiveSerializer {

	private Map<Class<?>, ObjectReplacer> from = new HashMap<Class<?>, ObjectReplacer>();
	private Map<Class<?>, ObjectReplacer> to = new HashMap<Class<?>, ObjectReplacer>();

	public <LocalType, RemoteType extends LocalType> void registerReplacer(Class<LocalType> from, Class<RemoteType> to, ObjectReplacer<LocalType, RemoteType> objectReplacer) {
		this.from.put(from, objectReplacer);
		this.to.put(to, objectReplacer);
	}

	public byte[] serialize(Object obj) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out = new ObjectOutputStream(bytes) {
				{
					if (!from.isEmpty()) {
						enableReplaceObject(true);
					}
				}

				@Override
				protected Object replaceObject(Object obj) throws IOException {
					for (Class<?> clazz : obj.getClass().getClasses()) {
						ObjectReplacer replacer = from.get(clazz);
						if (replacer != null) {
							return replacer.outgoing(obj);
						}
					}
					return super.replaceObject(obj);
				}
			};
			out.writeObject(obj);
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return bytes.toByteArray();
	}

	public Object deserialize(byte[] bytes) {
		try {
			return new ObjectInputStream(new ByteArrayInputStream(bytes)) {
				{
					if (!to.isEmpty()) {
						enableResolveObject(true);
					}
				}
				@Override
				protected Object resolveObject(Object obj) throws IOException {
					for (Class<?> clazz : obj.getClass().getClasses()) {
						ObjectReplacer replacer = to.get(clazz);
						if (replacer != null) {
							return replacer.incoming(obj);
						}
					}
					return obj;
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
