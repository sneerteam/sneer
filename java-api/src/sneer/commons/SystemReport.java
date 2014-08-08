package sneer.commons;

import rx.*;
import rx.subjects.*;

/** A simple monitoring tool. A single place for classes to report useful monitoring info and for that information to be consumed in a report. */
public class SystemReport {

	private static final BehaviorSubject<String> report = BehaviorSubject.create("");

	/** @return An observable that emits up-to-date reports about the system. */
	public static Observable<String> report() {
		return report.asObservable();
	}

	/** Causes report() above to emit an updated report with the given info. */
	public static void updateReport(String tag, String info) {
		//
	}

}
