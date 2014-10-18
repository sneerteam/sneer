package sneer.android.main.ipc;

import static sneer.SneerAndroidClient.ERROR;
import static sneer.SneerAndroidClient.TEXT;
import static sneer.SneerAndroidClient.PAYLOAD;
import static sneer.SneerAndroidClient.OWN;
import static sneer.SneerAndroidClient.PARTNER_NAME;
import static sneer.SneerAndroidClient.REPLAY_FINISHED;
import static sneer.SneerAndroidClient.RESULT_RECEIVER;
import static sneer.SneerAndroidClient.UNSUBSCRIBE;

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
import sneer.utils.SharedResultReceiver;
import sneer.utils.Value;
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
	private Sneer sneer;
	private PluginHandler plugin;
	private Context context;
	private CompositeSubscription subscriptions = new CompositeSubscription();

	
	PartnerSession(Context context, Sneer sneer, PluginHandler app) {
		this.context = context;
		this.sneer = sneer;
		this.plugin = app;
	}
	
	
	private void sendMessage(ResultReceiver toClient, Tuple t1) {
		Bundle data = new Bundle();
		data.putString(TEXT, (String) t1.get("text"));
		data.putBoolean(OWN, t1.author().equals(sneer.self().publicKey().current()));
		data.putParcelable(PAYLOAD, Value.of(t1.payload()));
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
	

	protected SharedResultReceiver createResultReceiver() {
		return new SharedResultReceiver(new SharedResultReceiver.Callback() { @Override public void call(Bundle resultData) {
			resultData.setClassLoader(context.getClassLoader());
			final ResultReceiver toClient = resultData.getParcelable(RESULT_RECEIVER);
			
			if (toClient != null) {
				setup(toClient);
			} else if(resultData.getBoolean(UNSUBSCRIBE)) {
				subscriptions.unsubscribe();
			} else {
				publish(resultData.getString(TEXT), ((Value)resultData.getParcelable(PAYLOAD)).get());
			}
		}});
	}
	

	private void setup(final ResultReceiver toClient) {
		pipePartnerName(toClient);
		pipeMessages(toClient);
	}
	
	
	private void pipeMessages(final ResultReceiver toClient) {
		subscriptions.add(queryTuples().localTuples()
			.subscribe(new Action1<Tuple>() { @Override public void call(Tuple t1) {
				lastLocalTuple = t1;
				sendMessage(toClient, t1);
			}},
			new Action1<Throwable>() { @Override public void call(Throwable t1) {
				sendError(toClient, t1);
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
			.type(plugin.tupleType())
			.audience(partner)
			.field("session", sessionId)
			.field("host", host)
			.field("text", text)
			.field("conversation?", true)
			.pub(message);
	}
	

	private TupleFilter queryTuples() {
		return sneer.tupleSpace().filter()
			.field("session", sessionId)
			.field("host", host)
			.type(plugin.tupleType());
	}
	

	private void pipeNewTuples(final ResultReceiver toClient) {
		subscriptions.add(queryTuples().tuples()
			.filter(new Func1<Tuple, Boolean>() { @Override public Boolean call(Tuple t1) {
				if (lastLocalTuple != null) {
					if (lastLocalTuple.equals(t1)) {
						lastLocalTuple = null;
					}
					return false;
				}
				return true;
			}})
			.subscribe(new Action1<Tuple>() { @Override public void call(Tuple t1) {
				sendMessage(toClient, t1);
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