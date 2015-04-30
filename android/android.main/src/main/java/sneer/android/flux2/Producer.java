package sneer.android.flux2;

import sneer.commons.Consumer;

public interface Producer<T> {

	void addConsumer(Lease lease, Consumer<T> consumer);

}
