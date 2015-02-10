package sneer.commons.tests;

import org.junit.Test;
import sneer.commons.InteractiveSerializer;
import sneer.commons.ObjectReplacer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class InteractiveSerializerTests {

	@Test
	public void simplest() {
		InteractiveSerializer s = new InteractiveSerializer();
		assertEquals("42",  s.deserialize(s.serialize("42")));
	}
	
	static interface Common {
		String value();
	}
	
	static class Local implements Common {
		static class NonSerializableClass {
			String value = "42";
		}
		
		NonSerializableClass field = new NonSerializableClass();

		@Override
		public String value() {
			return field.value;
		}
	}
	
	static class Remote implements Common, Serializable {
		private static final long serialVersionUID = 1L;
		
		String value;

		public Remote(String value) {
			this.value = value;
		}

		@Override
		public String value() {
			return value;
		}
		
	}
	
	@Test
	public void replaceInterface () {
		final Map<String, Common> localInstances = new HashMap<String, Common>();
		InteractiveSerializer replacing = new InteractiveSerializer();
		replacing.registerReplacer(Common.class, Remote.class, new ObjectReplacer<Common, Remote>() {

			@Override
			public Remote outgoing(Common local) {
				localInstances.put(local.value(), local);
				return new Remote(local.value());
			}

			@Override
			public Common incoming(Remote remote) {
				return localInstances.get(remote.value());
			}
		});
		
		Local local = new Local();
		assertSame(local, replacing.deserialize(replacing.serialize(local)));
	}

}
