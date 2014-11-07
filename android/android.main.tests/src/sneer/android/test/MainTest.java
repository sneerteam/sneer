package sneer.android.test;

import rx.Observable;
import rx.functions.Action1;
import sneer.android.ui.MainActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

public class MainTest extends ActivityInstrumentationTestCase2<MainActivity> {

	private Solo solo;

	public MainTest() {
		super(MainActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		// setUp() is run before a test case is started.
		// This is where the solo object is created.
		solo = new Solo(getInstrumentation(), getActivity());
	}

	@Override
	public void tearDown() throws Exception {
		// tearDown() is run after a test case has finished.
		// finishOpenedActivities() will finish all the activities that have
		// been opened during the test execution.
		solo.finishOpenedActivities();
	}

	public void testAddContact() throws Exception {
		// Unlock the lock screen
		solo.unlockScreen();

		solo.clickOnMenuItem("Advanced");
		// Assert that SystemReportActivity activity is opened
		solo.assertCurrentActivity("Expected SystemReport activity", "SystemReportActivity");

		backToConversationList();

		// Assert that MainActivity activity is opened
		solo.assertCurrentActivity("Expected MainActivity activity", "MainActivity");

		solo.clickOnButton("Add Contact");
		solo.clickOnButton("Send Public Key");

		backToConversationList();

		// Go to ConversationActivity
		solo.clickInList(1);

		sendSomeMessages();

//		// send location
		solo.clickOnImageButton(0);
		solo.clickInList(2);
//
		backToConversationList();
	}

	private void backToConversationList() {
		// Go back to first activity
		solo.goBackToActivity("MainActivity");
	}

	private void sendSomeMessages() {
		Observable.range(1, 10).subscribe(new Action1<Integer>() { @Override public void call(Integer t1) {
			solo.typeText(0, "This is a test " + t1);
			solo.clickOnImageButton(0);
		}});
	}

}
