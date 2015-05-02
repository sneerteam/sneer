package sneer.commons;

public interface ActionBus {

	public void action(Object action);


	class Sim implements ActionBus {

		@Override
		public void action(Object action) {
			System.out.println("Action: " + action);
		}
	}

}
