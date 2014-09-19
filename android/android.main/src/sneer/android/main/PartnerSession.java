package sneer.android.main;

import static sneer.SneerAndroidClient.ERROR;
import static sneer.SneerAndroidClient.LABEL;
import static sneer.SneerAndroidClient.MESSAGE;
import static sneer.SneerAndroidClient.OWN;
import static sneer.SneerAndroidClient.PARTNER_NAME;
import static sneer.SneerAndroidClient.REPLAY_FINISHED;
import static sneer.SneerAndroidClient.RESULT_RECEIVER;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.tuples.Tuple;
import sneer.tuples.TupleFilter;
import sneer.utils.SharedResultReceiver;
import sneer.utils.Value;
import android.app.Activity;
import android.os.Bundle;
import android.os.ResultReceiver;

public final class PartnerSession implements SharedResultReceiver.Callback {
	
	private final PublicKey host;
	private final PublicKey partner;
	private final long sessionId;
	private Tuple lastLocalTuple = null;
	private String tupleType;
	private ClassLoader classLoader;
	private Sneer sneer;

	PartnerSession(String tupleType, PublicKey host, PublicKey partner, long sessionId, ClassLoader classLoader, Sneer sneer) {
		this.host = host;
		this.partner = partner;
		this.sessionId = sessionId;
		this.classLoader = classLoader;
		this.sneer = sneer;
		this.tupleType = tupleType;
	}
	
	private void sendMessage(ResultReceiver toClient, Tuple t1) {
		Bundle data = new Bundle();
		data.putString(LABEL, (String) t1.get("label"));
		data.putBoolean(OWN, t1.author().equals(sneer.self().publicKey().current()));
		data.putParcelable(MESSAGE, Value.of(t1.payload()));
		toClient.send(0, data);
	}

	private void sendReplayFinished(ResultReceiver toClient) {
		Bundle data = new Bundle();
		data.putBoolean(REPLAY_FINISHED, true);
		toClient.send(0, data);
	}
	
	private void sendError(ResultReceiver toClient, Throwable t1) {
		Bundle data = new Bundle();
		data.putString(ERROR, "Internal error ("+t1.getMessage()+")");
		toClient.send(0, data);
	}
	
	private void sendPartnerName(ResultReceiver toClient, String partnerName) {
		Bundle bundle = new Bundle();
		bundle.putString(PARTNER_NAME, partnerName);
		toClient.send(Activity.RESULT_OK, bundle);
	}

	@Override
	public void call(Bundle t1) {
		t1.setClassLoader(classLoader);
		final ResultReceiver toClient = t1.getParcelable(RESULT_RECEIVER);

		if (toClient != null) {
			setup(toClient);
		} else {
			publish(t1.getString(LABEL), ((Value)t1.getParcelable(MESSAGE)).get());
		}
	}

	private void setup(final ResultReceiver toClient) {
		pipePartnerName(toClient);
		pipeMessages(toClient);
	}
	
	private void pipeMessages(final ResultReceiver toClient) {
		queryTuples().localTuples()
			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
				lastLocalTuple = t1;
				sendMessage(toClient, t1);
			}},
			new Action1<Throwable>() {  @Override public void call(Throwable t1) {
				sendError(toClient, t1);
			}},
			new Action0() {  @Override public void call() {
				sendReplayFinished(toClient);
				pipeNewTuples(toClient);
			} });
	}

	private void pipePartnerName(final ResultReceiver toClient) {
		sneer.produceParty(partner).name().subscribe(new Action1<String>() {  @Override public void call(String partnerName) {
			sendPartnerName(toClient, partnerName);
		}});
	}

	private void publish(String label, Object message) {
		sneer.tupleSpace().publisher()
			.type(tupleType)
			.audience(partner)
			.field("session", sessionId)
			.field("host", host)
			.field("label", label)
			.pub(message);
	}

	private TupleFilter queryTuples() {
		return sneer.tupleSpace().filter()
			.field("session", sessionId)
			.field("host", host)
			.type(tupleType);
	}

	private void pipeNewTuples(final ResultReceiver toClient) {
		queryTuples().tuples()
			.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple t1) {
				if (lastLocalTuple != null) {
					if (lastLocalTuple.equals(t1)) {
						lastLocalTuple = null;
					}
					return false;
				}
				return true;
			} })
			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
				sendMessage(toClient, t1);
			} });
	}
}