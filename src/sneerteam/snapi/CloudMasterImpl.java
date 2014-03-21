package sneerteam.snapi;

import java.util.HashMap;
import java.util.Map;

import sneerteam.api.ICloud;

public class CloudMasterImpl implements CloudMaster {

	private final Map<Object, ICloudImpl> cloudsById = new HashMap<Object, ICloudImpl>();
	
	@Override
	synchronized
	public void close() {
		for (ICloudImpl cloud : cloudsById.values())
			cloud.close();
		cloudsById.clear();
	}

	@Override
	synchronized
	public ICloud.Stub freshCloudFor(Object id) {
		ICloudImpl fresh = new ICloudImpl();
		ICloudImpl old = cloudsById.put(id, fresh);
		old.close();
		return fresh;
	}

	
	
}
