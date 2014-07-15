package sneer.android.main;

import sneer.*;
import sneer.simulator.*;
import android.app.*;

public class SneerApp extends Application {
	
	private Sneer model;

	public Sneer model() {
		if (model == null)
			model = new SneerSimulator();
//			model = new SneerImpl(this);
		return model;
	}
	
}
