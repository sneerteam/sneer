package sneerteam.snapi;

import sneerteam.api.ICloud;

public interface CloudMaster {

	void close();

	/** Returns a new ICloud. All previous IClouds with the same id are closed. */
	ICloud.Stub freshCloudFor(Object id);

}
