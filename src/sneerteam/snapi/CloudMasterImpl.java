package sneerteam.snapi;

import java.util.HashMap;
import java.util.Map;

import sneerteam.api.ICloud;
import sneerteam.network.Network;

public class CloudMasterImpl implements CloudMaster {

	private final Map<Object, ICloudImpl> cloudsById = new HashMap<Object, ICloudImpl>();

//	private final Network network;
	private       Network network;

/*	public CloudMasterImpl(Network network) {
		this.network = network;
	}
*/	
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
		//ToServer toServer
		
		ICloudImpl old = cloudsById.put(id, fresh);
		old.close();
		return fresh;
	}

	
	
}
