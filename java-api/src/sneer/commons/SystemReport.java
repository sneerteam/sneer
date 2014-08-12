package sneer.commons;

import java.util.*;
import java.util.Map.Entry;

import rx.Observable;
import rx.subjects.*;

/** A simple monitoring tool. A single place for classes to report useful monitoring info and for that information to be consumed in a report. */
public class SystemReport {

	private static final BehaviorSubject<String> subject = BehaviorSubject.create("");
	private static final SortedMap<String, String> infosByTag = new TreeMap<String, String>();

	
	/** @return An observable that emits up-to-date reports about the system. */
	public static Observable<String> report() {
		return subject.asObservable();
	}

	
	/** Causes report() above to emit an updated report with the given info.toString() associated with the given tag. */
	synchronized
	public static void updateReport(String tag, Object info) {
		infosByTag.put(tag, info.toString());
		subject.onNext(latestReport());
	}

	
	private static String latestReport() {
		StringBuilder ret = new StringBuilder();
		for (Entry<String, String> entry : infosByTag.entrySet())
			ret.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n\n");
		return ret.toString();
	}

}
