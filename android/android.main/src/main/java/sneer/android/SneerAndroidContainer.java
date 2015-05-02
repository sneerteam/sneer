package sneer.android;

import sneer.commons.Container;

import static sneer.commons.Container.withPadding;

public class SneerAndroidContainer {

	private static final Container container = initContainer();

	public static <T> T component(Class<T> componentInterface) {
		return container.produce(componentInterface);
	}

	private static Container initContainer() {
		System.out.println("TODO: Use a CoreComponentLoader if available.");
		return new Container(withPadding("sims.", "Sim"));  //Example: sims.sneer.conversationListSim
	}
}
