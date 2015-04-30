package sneer.android.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import sneer.commons.exceptions.Exceptions;

public class UncaughtExceptionReporter implements Thread.UncaughtExceptionHandler {

	private static final String FILE_NAME = "stack.trace";
	private static Context context;

	public static void start(Context context, String mailAddress) {
		Exceptions.check(UncaughtExceptionReporter.context == null);
		UncaughtExceptionReporter.context = context;

		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionReporter());
		emailPreviousException(mailAddress);
	}


	private final Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

	private UncaughtExceptionReporter() {}


	public void uncaughtException(Thread t, Throwable e) {
		String report = trace(t, e);

		try {
			FileOutputStream trace = context.openFileOutput(
					FILE_NAME, Context.MODE_PRIVATE);
			trace.write(report.getBytes());
			trace.close();
		} catch (IOException ioe) {
			//We can do nothing about this.
		}

		defaultUEH.uncaughtException(t, e);
	}

	private String trace(Thread t, Throwable e) {
		String report = "Thread: " + t.toString() + "\n" +
				        "--------- Stack trace ---------\n\n";

		while (true) {
			report += log(e.toString() + "\n\n");
			StackTraceElement[] arr = e.getStackTrace();
			for (StackTraceElement line : arr)
				report += log("    " + line.toString() + "\n");
			e = e.getCause();
			if (e != null)
				report += log("--------- Caused by ---------\n\n");
			else
				return report + "-------------------------------\n\n";
		}
	}


	private String log(Object line) {
		Log.e("Uncaught Exception", line.toString());
		return line.toString();
	}


	private static void emailPreviousException(final String mailAddress) {
		final String trace = readPreviousTrace();
		if (trace == null) return;

		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		String subject = "Report to Improve " + appName();
		String body =
				"Mail this to " + mailAddress +
						"\n\n" +
						trace +
						"\n\n";

		sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{mailAddress});
		sendIntent.putExtra(Intent.EXTRA_TEXT, body);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		sendIntent.setType("message/rfc822");

		Intent chooser = Intent.createChooser(sendIntent, "Title:");
		chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(chooser);
	}

	private static String appName() {
		return context.getApplicationContext().getApplicationInfo().name;
	}


	private static String readPreviousTrace() {
		String ret = "";
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(context.openFileInput(FILE_NAME)));
			String line;
			while ((line = reader.readLine()) != null)
				ret += line + "\n";
			context.deleteFile(FILE_NAME);
		} catch (Exception e) {
			return null;
		}
		return ret;
	}
}