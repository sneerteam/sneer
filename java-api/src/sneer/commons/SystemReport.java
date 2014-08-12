package sneer.commons;

import java.util.*;
import java.util.Map.Entry;

import org.ocpsoft.prettytime.*;

import rx.Observable;
import rx.subjects.*;

/** A simple monitoring tool. A single place for classes to report useful monitoring info and for that information to be consumed in a report. */
public class SystemReport {

	private static final BehaviorSubject<String> subject = BehaviorSubject.create("");
	private static final SortedMap<String, Object> infosByTag = new TreeMap<String, Object>();

	/** @return An observable that emits up-to-date reports about the system. */
	public static Observable<String> report() {
		return subject.asObservable();
	}

	
	/** Causes report() above to emit an updated report with the current time associated with the given event tag. */	
	public static void updateReport(String tag) {
		updateReport(tag, new Date());
	}

	
	/** Causes report() above to emit an updated report with the given info.toString() associated with the given tag. */	
	public static void updateReport(String tag, Object info) {
		subject.onNext(updateReportAndGetLatest(tag, info));
	}

	
	synchronized
	private static String updateReportAndGetLatest(String tag, Object info) {
		infosByTag.put(tag, info);
		return latestReport();
	}

	
	private static String latestReport() {
		StringBuilder ret = new StringBuilder();
		for (Entry<String, Object> entry : infosByTag.entrySet())
			ret.append(entry.getKey()).append(": ").append(pretty(entry.getValue())).append("\n\n");
		return ret.toString();
	}


	private static String pretty(Object info) {
		Object ret = info;
		if (ret instanceof Date)  
			ret = new PrettyTime().format((Date)ret);
		
		return ret.toString();
	}

}
