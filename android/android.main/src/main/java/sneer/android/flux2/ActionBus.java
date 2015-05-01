package sneer.android.flux2;

import android.util.Log;

public interface ActionBus {

	public void action(Object action);


	class Sim implements ActionBus {

		@Override
		public void action(Object action) {
			Log.d("ActionBus", action.toString());
		}
	}

}
