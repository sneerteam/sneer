package sneerteam.snapi.tests;

import static org.easymock.EasyMock.*;
import static sneerteam.snapi.CloudPath.*;

import java.util.Arrays;

import junit.framework.TestCase;

import org.easymock.Capture;

import rx.functions.Action1;
import sneerteam.api.ICloud;
import sneerteam.api.ISubscriber;
import sneerteam.api.ISubscription;
import sneerteam.api.Value;
import sneerteam.snapi.*;

public class CloudConnectionTest extends TestCase {
	
	public void testIsLazyToSub() {
		ICloud cloud = createMock(ICloud.class);
		replay(cloud);
		
		CloudConnection subject = new CloudConnection(cloud);
		subject.path(ME, "contacts").children();
	}
	
	public void testUnsubscribeCausesDispose() throws Exception {
		ICloud cloud = createMock(ICloud.class);
		ISubscription subscription = createStrictMock(ISubscription.class);
		expect(cloud.sub(eqPath(ME, "contacts"), anyObject(ISubscriber.class)))
		  .andReturn(subscription);
		subscription.dispose();
		replay(cloud, subscription);
		
		CloudConnection subject = new CloudConnection(cloud);
		subject.path(ME, "contacts").children().subscribe().unsubscribe();
		
		verify(cloud, subscription);
	}

	public void testValueIsObservable() throws Exception {
		final String expectedNick = "ana";
		
		ICloud cloud = createMock(ICloud.class);
		ISubscription subscription = createMock(ISubscription.class);		
		Capture<ISubscriber> subscriber = new Capture<ISubscriber>();
		expect(cloud.sub(eqPath(ME, "nick"), capture(subscriber)))
		  .andReturn(subscription);
		
		@SuppressWarnings("unchecked")
		Action1<String> onNext = createMock(Action1.class);
		onNext.call(expectedNick);
		
		subscription.dispose();		
		replay(cloud, subscription, onNext);
		
		CloudConnection subject = new CloudConnection(cloud);
		subject
			.path(ME, "nick")
			.value()
			.cast(String.class)
			.first()
			.subscribe(onNext);
		
		subscriber.getValue().onValue(path(ME, "nick"), value(expectedNick));
		
		verify(cloud, subscription, onNext);
	}
	
	public void testValueIsObserver() throws Exception {
		ICloud cloud = createMock(ICloud.class);
		cloud.pubValue(eqPath(ME, "nick"), eq(value("bob")));
		replay(cloud);
		
		CloudConnection subject = new CloudConnection(cloud);
		subject
			.path(ME, "nick")
			.value()
			.onNext("bob");
		
		verify(cloud);
	}
	
	public void testPathPub() throws Exception {
		ICloud cloud = createMock(ICloud.class);
		cloud.pubPath(eqPath(ME, "nick"));
		replay(cloud);
		
		CloudConnection subject = new CloudConnection(cloud);
		subject.path(ME).append("nick").pub();
		
		verify(cloud);
	}
	
	public void testPathAppend() throws Exception {
		ICloud cloud = createMock(ICloud.class);
		ISubscription subscription = createMock(ISubscription.class);
		expect(cloud.sub(eqPath(ME, "contacts"), anyObject(ISubscriber.class)))
		  .andReturn(subscription);
		subscription.dispose();
		replay(cloud, subscription);
		
		CloudConnection subject = new CloudConnection(cloud);
		subject.path(ME).append("contacts").children().subscribe().unsubscribe();
		
		verify(cloud, subscription);
	}

	private Value value(String value) {
		return Value.of(value);
	}

	private Value[] eqPath(Object... segments) {
		return aryEq(path(segments));
	}

	private Value[] path(Object... segments) {
		return Encoder.pathEncode(Arrays.asList(segments));
	}
}
