package sneer.core.tests;

import java.io.*;

import org.junit.*;
import sneer.*;

public class DisposableProtocolTest {
	
	class Resource implements Closeable {
		public boolean closed;

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
