package sneer.android.ipc;

import static sneer.android.impl.IPCProtocol.ERROR;
import static sneer.android.impl.IPCProtocol.OWN;
import static sneer.android.impl.IPCProtocol.PARTNER_NAME;
import static sneer.android.impl.IPCProtocol.PAYLOAD;
import static sneer.android.impl.IPCProtocol.REPLAY_FINISHED;
import static sneer.android.impl.IPCProtocol.RESULT_RECEIVER;
import static sneer.android.impl.IPCProtocol.LABEL;
import static sneer.android.impl.IPCProtocol.UNSUBSCRIBE;

import java.util.concurrent.atomic.AtomicLong;

import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.commons.Clock;
import sneer.tuples.Tuple;
import sneer.tuples.TupleFilter;
import sneer.android.impl.SharedResultReceiver;
import sneer.android.impl.Value;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public final class PartnerSession implements PluginSession {

	private static AtomicLong nextSessionId = new AtomicLong(Clock.now());

	private PublicKey host;
	private PublicKey partner;
	private long sessionId;
	private Tuple lastLocalTuple = null;
	private final Sneer sneer;
	private final PluginHandler plugin;
	private final Context context;
	private final CompositeSubscription subscriptions = new CompositeSubscription();


	PartnerSession(Context context, Sneer sneer, PluginHandler app) {
		this.context = context;
		this.sneer = sneer;
		plugin = app;
	}


	private void sendMessage(ResultReceiver toClient, Tuple tuple) {
		Bundle data = new Bundle();
		data.putString(LABEL, (String) tuple.get("text"));
		data.putBoolean(OWN, tuple.author().equals(sneer.self().publicKey().current()));
		data.putParcelable(PAYLOAD, Value.of(tuple.payload()));
		toClient.send(0, data);
	}


	private void sendReplayFinished(ResultReceiver toClient) {
		Bundle data = new Bundle();
		data.putBoolean(REPLAY_FINISHED, true);
		toClient.send(0, data);
	}


	private void sendError(ResultReceiver toClient, Throwable throwable) {
		Bundle data = new Bundle();
		data.putString(ERROR, "Internal error (" + throwable.getMessage() + ")");
		toClient.send(0, data);
	}


	private void sendPartnerName(ResultReceiver toClient, String partnerName) {
		Bundle bundle = new Bundle();
		bundle.putString(PARTNER_NAME, partnerName);
		toClient.send(Activity.RESULT_OK, bundle);
	}


	protected SharedResultReceiver createResultReceiver() {
		return new SharedResultReceiver(new SharedResultReceiver.Callback() { @Override public void call(Bundle resultData) {
			resultData.setClassLoader(context.getClassLoader());
			final ResultReceiver toClient = resultData.getParcelable(RESULT_RECEIVER);

			if (toClient != null)
				setup(toClient);
			else if (resultData.getBoolean(UNSUBSCRIBE))
				subscriptions.unsubscribe();
			else
				publish(resultData.getString(LABEL), ((Value)resultData.getParcelable(PAYLOAD)).get());
		}});
	}


	private void setup(final ResultReceiver toClient) {
		pipePartnerName(toClient);
		pipeMessages(toClient);
	}


	private void pipeMessages(final ResultReceiver toClient) {
		subscriptions.add(queryTuples().localTuples()
			.subscribe(new Action1<Tuple>() { @Override public void call(Tuple tuple) {
				lastLocalTuple = tuple;
				sendMessage(toClient, tuple);
			}},
			new Action1<Throwable>() { @Override public void call(Throwable throwable) {
				sendError(toClient, throwable);
			}},
			new Action0() { @Override public void call() {
				sendReplayFinished(toClient);
				pipeNewTuples(toClient);
			}}));
	}


	private void pipePartnerName(final ResultReceiver toClient) {
		subscriptions.add(sneer.produceParty(partner).name().subscribe(new Action1<String>() { @Override public void call(String partnerName) {
			sendPartnerName(toClient, partnerName);
		}}));
	}


	private void publish(String text, Object message) {
		sneer.tupleSpace().publisher()
			.type("message")
			.field("message-type", plugin.tupleType())
			.audience(partner)
			.field("session", sessionId)
			.field("host", host)
			.field("text", text)
			.pub(message);
	}


	private TupleFilter queryTuples() {
		return sneer.tupleSpace().filter()
			.field("session", sessionId)
			.field("host", host)
			.field("message-type", plugin.tupleType());
	}


	private void pipeNewTuples(final ResultReceiver toClient) {
		subscriptions.add(queryTuples().tuples()
			.filter(new Func1<Tuple, Boolean>() { @Override public Boolean call(Tuple tuple) {
				if (lastLocalTuple != null) {
					if (lastLocalTuple.equals(tuple)) {
						lastLocalTuple = null;
					}
					return false;
				}
				return true;
			}})
			.subscribe(new Action1<Tuple>() { @Override public void call(Tuple tuple) {
				sendMessage(toClient, tuple);
			}}));
	}


	private void startActivity() {
		context.startActivity(createIntent());
	}


	private Intent createIntent() {
		Intent intent = plugin.createIntent();
		intent.putExtra(RESULT_RECEIVER, createResultReceiver());
		return intent;
	}


	@Override
	public Intent createResumeIntent(Tuple tuple) {
		sessionId = (Long) tuple.get("session");
		host = (PublicKey) tuple.get("host");
		partner = tuple.author().equals(sneer.self().publicKey().current()) ? tuple.audience() : tuple.author();
		return createIntent();
	}


	@Override
	public void startNewSessionWith(PublicKey partner) {
		host = sneer.self().publicKey().current();
		sessionId = nextSessionId.getAndIncrement();
		this.partner = partner;
		startActivity();
	}

}