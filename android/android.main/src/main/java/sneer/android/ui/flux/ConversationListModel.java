package sneer.android.ui.flux;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import sneer.android.SneerAndroidSingleton;
import sneer.flux.ConversationStore;

public class ConversationListModel {

	public static Observable<List<Item>> items() {

		Observable<List<ConversationStore.Summary>> summaries = false ? fakeSummaries() : summaries();
		Observable<Long> reformattingTick = Observable.timer(0, 1, TimeUnit.MINUTES);

		return Observable.combineLatest(summaries, reformattingTick, ConversationListModel.<List<ConversationStore.Summary>>firstArg()).map(
			new Func1<List<ConversationStore.Summary>, List<Item>>() {@Override	public List<Item> call(List<ConversationStore.Summary> summaries) {
				return format(summaries);
			}});

	}

	private static Observable<List<ConversationStore.Summary>> summaries() {
		//return store().summaries();
		return null;
	}

	private static ConversationStore store() {
//		return Dispatcher.createInstance(ConversationStore.class, SneerAndroidSingleton.admin());
		return null;
	}

	private static Observable<List<ConversationStore.Summary>> fakeSummaries() {
		long timestamp = now();
		long seconds = 1000;
		long hours = 60 * seconds;
		ConversationStore.Summary[] initial = {
			new ConversationStore.Summary("neide", "oi", timestamp - 3 * seconds, 1),
			new ConversationStore.Summary("maico", "pois é", timestamp - 5 * hours, 2),
			new ConversationStore.Summary("alice", "oi", timestamp - 24 * hours, 0)
		};
		return Observable
			.timer(0, 3, TimeUnit.SECONDS)
			.scan(
				Arrays.asList(initial),
				new Func2<List<ConversationStore.Summary>, Long, List<ConversationStore.Summary>>() {@Override public List<ConversationStore.Summary> call(List<ConversationStore.Summary> previous, Long tick) {
					ConversationStore.Summary newItem = new ConversationStore.Summary("Contact " + tick, "another challenger appears!", now(), tick % 5);
					ArrayList<ConversationStore.Summary> next = new ArrayList<>();
					next.add(newItem);
					next.addAll(previous);
					return next;
				}});
	}

	static class Item {
		public final String party;
		public final String textPreview;
		public final String date;
		public final String unread;

		Item(String party, String textPreview, String date, String unread) {
			this.party = party;
			this.textPreview = textPreview;
			this.date = date;
			this.unread = unread;
		}
	}

	private static List<Item> format(List<ConversationStore.Summary> summaries) {
		PrettyTime pt = new PrettyTime();
		ArrayList<Item> items = new ArrayList<>(summaries.size());
		for (ConversationStore.Summary summary : summaries)
			items.add(
				new Item(
					summary.party,
					summary.textPreview,
					pt.format(new Date(summary.timestamp)),
					summary.unread > 0 ? Long.toString(summary.unread) : ""));
		return items;
	}

	private static long now() {
		return Calendar.getInstance().getTimeInMillis();
	}

	private static <T> Func2<T, Object, T> firstArg() {
		return new Func2<T, Object, T>() { public T call(T first, Object second) {
			return first;
		}};
	}

}
