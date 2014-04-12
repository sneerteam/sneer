package sneerteam.snapi;

import java.util.Arrays;
import java.util.List;

import sneerteam.api.ICloud;

public class CloudConnection {
	ICloud cloud;

	public CloudConnection(ICloud cloud) {
		this.cloud = cloud;
	}
	
	public Path path(Object... segments) {
		return path(Arrays.asList(segments));
	}
	
	public Path path(List<Object> segments) {
		return new Path(this, segments);
	}
}
