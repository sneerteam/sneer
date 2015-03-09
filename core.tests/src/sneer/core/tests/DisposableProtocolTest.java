package sneer.core.tests;

import org.junit.Assert;
import org.junit.Test;

import java.io.Closeable;

public class DisposableProtocolTest {

	class Resource implements Closeable {
		public boolean closed;

		@Override
		public void close() {
			closed = true;
		}
	}


 	@Test
	public void disposeDisposesCloseable() {
		Resource resource = new Resource();
		ClojureUtils.dispose(resource);
		Assert.assertTrue(resource.closed);
	}

}
