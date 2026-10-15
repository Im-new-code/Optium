package me.modmuss50.optium.mod;

import java.io.PrintWriter;
import java.io.StringWriter;

public class OptiumError {
	private static String error;
	private static String stack;

	public static boolean hasError() {
		return error != null;
	}

	public static String getError() {
		return error;
	}

	public static void setError(String error) {
		OptiumError.error = error;
	}

	public static void setError(Throwable t, String error) {
		OptiumError.error = error;
		logError(t);
	}

	public static void setError(String error, Object... args) {
		OptiumError.error = String.format(error, args);
	}

	public static void logError(Throwable t) {
		StringWriter error = new StringWriter();
		t.printStackTrace(new PrintWriter(error));
		stack = error.toString();
	}

	public static String getErrorLog() {
		return stack;
	}
}