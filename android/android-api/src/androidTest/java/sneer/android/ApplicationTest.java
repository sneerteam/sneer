package sneer.android;

import android.app.Application;
import android.test.ApplicationTestCase;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
	public ApplicationTest() {
		super(Application.class);

		stimulateApiToRemoveUnusedWarnings();
	}

	private void stimulateApiToRemoveUnusedWarnings() {
		PartnerSession session = PartnerSession.join(null, null);
		session.wasStartedByMe();
		session.send(null);
	}
}